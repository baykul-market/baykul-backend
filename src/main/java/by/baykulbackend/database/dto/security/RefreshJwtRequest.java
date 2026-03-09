package by.baykulbackend.database.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(
        description = "Refresh token request",
        examples = {
            """
            {
              "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huX2RvZSIsImlhdCI6MTcxNjIzOTAyMiwiZXhwIjoxNzE4ODMxMDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
            }
            """
        }
)
public class RefreshJwtRequest {

    @NotBlank(message = "Refresh token is required")
    @Schema(
            description = "Refresh token",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huX2RvZSIsIm..."
    )
    public String refreshToken;
}
