package by.baykulbackend.database.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(
        description = "Authentication request",
        examples = {
                """
                {
                  "login": "john_doe",
                  "password": "securePassword123"
                }
                """
        }
)
public class JwtRequest {

    @NotBlank(message = "Login is required")
    @Schema(
            description = "Username",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "john_doe"
    )
    private String login;

    @NotBlank(message = "Password is required")
    @Schema(
            description = "Password",
            requiredMode = Schema.RequiredMode.REQUIRED,
            format = "password",
            example = "securePassword123"
    )
    private String password;
}