package by.baykulbackend.services.user;

import by.baykulbackend.database.dao.user.RefreshToken;
import by.baykulbackend.database.dao.user.User;
import by.baykulbackend.security.JwtAuthentication;
import by.baykulbackend.database.repository.user.IRefreshTokenRepository;
import by.baykulbackend.database.repository.user.IUserRepository;
import by.baykulbackend.exceptions.JwtAuthenticationException;
import by.baykulbackend.security.JwtProvider;
import by.baykulbackend.database.dto.security.JwtRequest;
import by.baykulbackend.database.dto.security.JwtResponse;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final IRefreshTokenRepository iRefreshTokenRepository;
    private final IUserRepository iUserRepository;
    private final RequestService requestService;

    /**
     * Authenticates a user by login and password.
     * Generates new JWT access and refresh tokens upon successful authentication.
     *
     * @param userAgent   client/browser identifier from User-Agent header
     * @param request     HTTP request to extract client IP address
     * @param authRequest object containing user login and password
     * @return JwtResponse with access and refresh tokens
     * @throws JwtAuthenticationException if:
     *         1. User not found
     *         2. User is blocked
     *         3. Invalid password
     */
    public JwtResponse login(String userAgent, @NonNull HttpServletRequest request, @NonNull JwtRequest authRequest) {
        final User user = iUserRepository.findByLogin(authRequest.getLogin())
                .orElseThrow(() -> new JwtAuthenticationException("User not found", HttpStatus.FORBIDDEN));

        if (user.getBlocked()) {
            log.warn("The user = {} with id = {} is blocked. Authorization failed", user.getLogin(), user.getId());
            throw new JwtAuthenticationException("The user " + user.getLogin() + "is blocked. Authorization failed",
                    HttpStatus.FORBIDDEN);
        }

        if (passwordEncoder.matches(authRequest.getPassword(), user.getPassword())) {
            final String accessToken = jwtProvider.generateAccessToken(user);
            final String refreshToken = jwtProvider.generateRefreshToken(user);
            String clientIp = requestService.getClientIp(request);

            RefreshToken existingRefToken =
                    iRefreshTokenRepository.findRefreshTokenByUserAgentAndIpAddress(userAgent, clientIp);

            if (existingRefToken == null) {
                RefreshToken refToken = new RefreshToken();
                refToken.setUser(user);
                refToken.setName(refreshToken);
                refToken.setUserAgent(userAgent);
                refToken.setIpAddress(clientIp);
                iUserRepository.save(user);
                iRefreshTokenRepository.save(refToken);
            } else {
                existingRefToken.setName(refreshToken);
                iRefreshTokenRepository.save(existingRefToken);
            }

            return new JwtResponse(accessToken, refreshToken);
        } else {
            log.warn("Invalid password. User = {} with id = {}", user.getLogin(), user.getId());
            throw new JwtAuthenticationException("Invalid password", HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Generates a new access token using a valid refresh token.
     * Used when the access token has expired.
     * Does not generate a new refresh token (refresh token remains unchanged).
     *
     * @param refreshToken valid refresh token
     * @param request      HTTP request to extract client IP address
     * @return JwtResponse with new access token (refresh token is null in response)
     *         or empty JwtResponse if refresh token is invalid
     * @throws JwtAuthenticationException if:
     *         1. Refresh token is invalid
     *         2. User not found
     *         3. Refresh token doesn't exist in database
     */
    public JwtResponse getAccessToken(@NonNull String refreshToken, @NonNull HttpServletRequest request) {
        if (jwtProvider.validateRefreshToken(refreshToken)) {
            final Claims claims = jwtProvider.getRefreshClaims(refreshToken);
            final String login = claims.getSubject();
            final String saveRefreshToken = iRefreshTokenRepository.findRefreshTokenByName(refreshToken).getName();

            if (saveRefreshToken != null && saveRefreshToken.equals(refreshToken)) {
                final User user = iUserRepository.findByLogin(login)
                        .orElseThrow(() -> new JwtAuthenticationException("User not found", HttpStatus.NOT_FOUND));
                final String accessToken = jwtProvider.generateAccessToken(user);
                String clientIp = requestService.getClientIp(request);

                RefreshToken refreshTokenFromDb = iRefreshTokenRepository.findRefreshTokenByName(refreshToken);
                refreshTokenFromDb.setIpAddress(clientIp);
                iRefreshTokenRepository.save(refreshTokenFromDb);
                iUserRepository.save(user);

                return new JwtResponse(accessToken, null);
            }
        }

        return new JwtResponse(null, null);
    }

    /**
     * Rotates both access and refresh tokens.
     * Generates new access and refresh tokens, replacing the old refresh token in database.
     * Used for token rotation security practice.
     *
     * @param refreshToken current valid refresh token
     * @param request      HTTP request to extract client IP address
     * @return JwtResponse with new access and refresh tokens
     * @throws JwtAuthenticationException if:
     *         1. Refresh token is invalid
     *         2. User not found
     *         3. Refresh token doesn't exist in database
     */
    public JwtResponse refresh(@NonNull String refreshToken, @NonNull HttpServletRequest request) {
        if (jwtProvider.validateRefreshToken(refreshToken)) {
            final Claims claims = jwtProvider.getRefreshClaims(refreshToken);
            final String login = claims.getSubject();
            final RefreshToken refreshTokenFromDb = iRefreshTokenRepository.findRefreshTokenByName(refreshToken);

            if (refreshTokenFromDb.getName() != null && refreshTokenFromDb.getName().equals(refreshToken)) {
                final User user = iUserRepository.findByLogin(login)
                        .orElseThrow(() -> new JwtAuthenticationException("User not found", HttpStatus.FORBIDDEN));
                final String newAccessToken = jwtProvider.generateAccessToken(user);
                final String newRefreshToken = jwtProvider.generateRefreshToken(user);
                String clientIp = requestService.getClientIp(request);

                refreshTokenFromDb.setName(newRefreshToken);
                refreshTokenFromDb.setIpAddress(clientIp);
                iRefreshTokenRepository.save(refreshTokenFromDb);

                return new JwtResponse(newAccessToken, newRefreshToken);
            }
        }

        throw new JwtAuthenticationException("JWT token is invalid", HttpStatus.FORBIDDEN);
    }

    /**
     * Retrieves authentication information of the current user from SecurityContext.
     *
     * @return JwtAuthentication object with user data (login, role, ID)
     * @throws ClassCastException if security context contains non-JwtAuthentication object
     */
    public JwtAuthentication getAuthInfo() {
        return (JwtAuthentication) SecurityContextHolder.getContext().getAuthentication();
    }
}
