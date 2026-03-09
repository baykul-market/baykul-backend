package by.baykulbackend.database.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(
        description = "JWT tokens response",
        examples = {
                """
                {
                  "type": "Bearer",
                  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huX2RvZSIsImlhdC...",
                  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huX2RvZSIsImlhdC..."
                }
                """,
                """
                {
                  "type": "Bearer",
                  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huX2RvZSIsImlhdC...",
                  "refreshToken": null
                }
                """
        }
)
public class JwtResponse {

    @Schema(
            description = "Token type",
            defaultValue = "Bearer",
            example = "Bearer"
    )
    private final String type = "Bearer";

    @Schema(
            description = "Access token for API authorization (valid for 5 minutes)",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huX2RvZSIsImlhdC..."
    )
    private String accessToken;

    @Schema(
            description = "Refresh token for obtaining new access tokens (valid for 30 days)",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huX2RvZSIsImlhdC..."
    )
    private String refreshToken;
}
