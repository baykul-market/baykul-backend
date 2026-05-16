package by.baykulbackend.database.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request to reset forgotten password")
public class ForgotPasswordRequest {
    @NotBlank(message = "Identifier (login or email) is required")
    @Schema(
            description = "User's login or email address",
            example = "john_doe"
    )
    private String identifier;
}
