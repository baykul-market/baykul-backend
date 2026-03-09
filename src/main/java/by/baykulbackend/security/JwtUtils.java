package by.baykulbackend.security;

import by.baykulbackend.database.model.Role;
import io.jsonwebtoken.Claims;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JwtUtils {

    /**
     * Creates a JwtAuthentication object from JWT claims.
     * Extracts user role, ID, and login from token claims.
     *
     * @param claims the JWT claims/payload
     * @return JwtAuthentication object populated with user information
     */
    public static JwtAuthentication generate(Claims claims) {
        final JwtAuthentication jwtInfoToken = new JwtAuthentication();
        jwtInfoToken.setRole(getRole(claims));
        jwtInfoToken.setId(claims.get("id", String.class));
        jwtInfoToken.setLogin(claims.getSubject());

        return jwtInfoToken;
    }

    /**
     * Extracts user role from JWT claims.
     *
     * @param claims the JWT claims containing role information
     * @return Role enum value
     * @throws IllegalArgumentException if role claim is invalid
     */
    private static Role getRole(Claims claims) {
        final String role = claims.get("role", String.class);
        return Enum.valueOf(Role.class, role);
    }
}
