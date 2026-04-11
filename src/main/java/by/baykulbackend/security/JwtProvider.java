package by.baykulbackend.security;

import by.baykulbackend.database.dao.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Slf4j
@Component
public class JwtProvider {
    private final SecretKey jwtAccessSecret;
    private final SecretKey jwtRefreshSecret;

    public JwtProvider(
            @Value("${jwt.secret.access}") String jwtAccessSecret,
            @Value("${jwt.secret.refresh}") String jwtRefreshSecret
    ) {
        this.jwtAccessSecret = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtAccessSecret));
        this.jwtRefreshSecret = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtRefreshSecret));
    }

    /**
     * Generates a short-lived JWT access token for a user.
     * Token contains user login, role, ID, and expiration time (5 minutes).
     *
     * @param user the user for whom to generate the token
     * @return JWT access token as String, or null if user is blocked
     */
    public String generateAccessToken(@NonNull User user) {
        final LocalDateTime now = LocalDateTime.now();
        final Instant accessExpirationInstant = now.plusMinutes(5).atZone(ZoneId.systemDefault()).toInstant();
        final Date accessExpiration = Date.from(accessExpirationInstant);

        if (user.getBlocked()) {
            return null;
        }

        return Jwts.builder()
                .subject(user.getLogin())
                .expiration(accessExpiration)
                .signWith(jwtAccessSecret)
                .claim("role", user.getRole())
                .claim("id", user.getId())
                .compact();
    }

    /**
     * Generates a long-lived JWT refresh token for a user.
     * Token contains only user login and expiration time (30 days).
     *
     * @param user the user for whom to generate the token
     * @return JWT refresh token as String, or null if user is blocked
     */
    public String generateRefreshToken(@NonNull User user) {
        final LocalDateTime now = LocalDateTime.now();
        final Instant refreshExpirationInstant = now.plusDays(30).atZone(ZoneId.systemDefault()).toInstant();
        final Date refreshExpiration = Date.from(refreshExpirationInstant);

        if (user.getBlocked()) {
            return null;
        }

        return Jwts.builder()
                .subject(user.getLogin())
                .expiration(refreshExpiration)
                .signWith(jwtRefreshSecret)
                .compact();
    }

    /**
     * Validates an access token's signature and expiration.
     *
     * @param accessToken the JWT access token to validate
     * @return true if token is valid, false otherwise
     */
    public boolean validateAccessToken(@NonNull String accessToken) {
        return validateToken(accessToken, jwtAccessSecret);
    }

    /**
     * Validates a refresh token's signature and expiration.
     *
     * @param refreshToken the JWT refresh token to validate
     * @return true if token is valid, false otherwise
     */
    public boolean validateRefreshToken(@NonNull String refreshToken) {
        return validateToken(refreshToken, jwtRefreshSecret);
    }

    /**
     * Generic token validation method.
     *
     * @param token  the JWT token to validate
     * @param secret the secret key used for signature verification
     * @return true if token is valid, false for any validation error
     */
    private boolean validateToken(@NonNull String token, @NonNull SecretKey secret) {
        try {
            Jwts.parser()
                    .verifyWith(secret)
                    .build()
                    .parseSignedClaims(token);

            return true;
        } catch (ExpiredJwtException expEx) {
            log.error("Token expired");
        } catch (UnsupportedJwtException unsEx) {
            log.error("Unsupported jwt");
        } catch (MalformedJwtException mjEx) {
            log.error("Malformed jwt");
        } catch (SignatureException sEx) {
            log.error("Invalid signature");
        } catch (Exception e) {
            log.error("Invalid token");
        }

        return false;
    }

    /**
     * Extracts claims (payload) from an access token.
     *
     * @param token the JWT access token
     * @return Claims object containing token payload
     * @throws JwtException if token is invalid
     */
    public Claims getAccessClaims(@NonNull String token) {
        return getClaims(token, jwtAccessSecret);
    }

    /**
     * Extracts claims (payload) from a refresh token.
     *
     * @param token the JWT refresh token
     * @return Claims object containing token payload
     * @throws JwtException if token is invalid
     */
    public Claims getRefreshClaims(@NonNull String token) {
        return getClaims(token, jwtRefreshSecret);
    }

    /**
     * Extracts claims (payload) from a token.
     *
     * @param token  the JWT token
     * @param secret the secret key used for signature verification
     * @return Claims object containing token payload
     * @throws JwtException if token is invalid
     */
    private Claims getClaims(@NonNull String token, @NonNull SecretKey secret) {
        return Jwts.parser()
                .verifyWith(secret)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
