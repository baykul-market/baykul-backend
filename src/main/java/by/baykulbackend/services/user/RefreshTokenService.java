package by.baykulbackend.services.user;

import by.baykulbackend.database.dao.user.RefreshToken;
import by.baykulbackend.database.dao.user.User;
import by.baykulbackend.database.model.Role;
import by.baykulbackend.database.repository.user.IRefreshTokenRepository;
import by.baykulbackend.database.repository.user.IUserRepository;
import by.baykulbackend.exceptions.BadRequestException;
import by.baykulbackend.exceptions.NotFoundException;
import by.baykulbackend.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenService {
    private final IRefreshTokenRepository iRefreshTokenRepository;
    private final AuthService authService;
    private final IUserRepository iUserRepository;
    private final JwtProvider jwtProvider;

    /**
     * Retrieves all refresh tokens for the currently authenticated user.
     *
     * @return List of RefreshToken objects belonging to the current user
     */
    public List<RefreshToken> findUserRefreshTokens() {
        User userFromDB = iUserRepository.findByLogin(authService.getAuthInfo().getPrincipal().toString())
                .orElseThrow(() -> new NotFoundException("User not found"));

        return iRefreshTokenRepository.findRefreshTokenByUser(userFromDB);
    }

    /**
     * Retrieves all refresh tokens for a specific user by their ID.
     *
     * @param id the UUID of the user
     * @return List of RefreshToken objects belonging to the specified user
     * @throws NotFoundException if no user is found with the given ID
     */
    public List<RefreshToken> findUserRefTokensByUserId(UUID id) {
        User userFromDB = iUserRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));

        return iRefreshTokenRepository.findRefreshTokenByUser(userFromDB);
    }

    /**
     * Deletes a refresh token by its ID.
     * Only allows deletion if the current user owns the token or is an ADMIN.
     *
     * @param id the UUID of the refresh token to delete
     * @return ResponseEntity with success/error message
     * @throws NotFoundException if no refresh token is found with the given ID
     */
    public ResponseEntity<?> deleteById(UUID id) {
        Map<String, String> response = new HashMap<>();
        RefreshToken refreshTokenFromDB = iRefreshTokenRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Refresh token not found"));
        User userFromDB = iUserRepository.findByLogin(authService.getAuthInfo().getPrincipal().toString())
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (userFromDB.equals(refreshTokenFromDB.getUser()) || userFromDB.getRole().equals(Role.ADMIN)) {
            iRefreshTokenRepository.deleteById(id);
            response.put("delete_refresh_token", "true");
            log.info("Delete refresh token with id = {} -> {}", id, authService.getAuthInfo().getPrincipal());

            return ResponseEntity.ok(response);
        }

        response.put("delete_refresh_token", "false");
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Deletes a refresh token by its ID.
     * Only allows deletion if the current user owns the token or is an ADMIN.
     *
     * @param refreshToken the refresh token to delete
     * @throws NotFoundException if no refresh token is found
     * @throws BadRequestException if token is invalid
     */
    public void deleteByName(String refreshToken) {
        if (!jwtProvider.validateRefreshToken(refreshToken)) {
            throw new BadRequestException("Invalid refresh token");
        }

        RefreshToken refreshTokenFromDB = Optional.ofNullable(iRefreshTokenRepository.findRefreshTokenByName(refreshToken))
                .orElseThrow(() -> new NotFoundException("Refresh token not found"));
        User userFromDB = iUserRepository.findByLogin(authService.getAuthInfo().getPrincipal().toString())
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (userFromDB.equals(refreshTokenFromDB.getUser()) || userFromDB.getRole().equals(Role.ADMIN)) {
            iRefreshTokenRepository.deleteById(refreshTokenFromDB.getId());
            log.info("Delete refresh token {} -> {}", refreshToken, authService.getAuthInfo().getPrincipal());
        }
    }
}
