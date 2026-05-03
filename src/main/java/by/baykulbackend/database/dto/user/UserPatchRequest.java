package by.baykulbackend.database.dto.user;

import by.baykulbackend.database.dao.user.Localization;
import by.baykulbackend.database.dao.user.Profile;
import by.baykulbackend.database.model.Role;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * DTO used exclusively for admin PATCH /api/v1/users/id requests.
 *
 * <p>All fields are optional. Only fields that are present in the JSON body are applied.
 * Fields absent from the body are silently ignored.
 *
 * <p>{@link #markupPercentage} supports three distinct states thanks to
 * {@link NullableDecimalDeserializer}:
 * <ul>
 *   <li><b>Absent</b> — do not touch the stored value.</li>
 *   <li><b>JSON {@code null}</b> — clear to {@code null}, activating global config fallback.</li>
 *   <li><b>Numeric value</b> — set to that value.</li>
 * </ul>
 */
@Data
@Schema(description = "Admin user patch request. All fields are optional; only provided fields are applied.")
public class UserPatchRequest {

    @Schema(description = "Login name (3–50 chars)", example = "john_doe")
    @Size(min = 3, max = 50, message = "The login must be between 3 and 50 characters")
    @Pattern(regexp = "^(?!\\s*$).+", message = "Login must not be empty")
    private String login;

    @Schema(description = "New plain-text password (8–100 chars)", example = "newSecurePass123")
    @Size(min = 8, max = 100, message = "The password must be between 8 and 100 characters")
    @Pattern(regexp = "^(?!\\s*$).+", message = "The password must not be empty")
    private String password;

    @Schema(description = "Email address", example = "john@example.com")
    private String email;

    @Schema(description = "Phone number", example = "+375291234567")
    @Pattern(regexp = "^\\+[1-9]\\d{6,14}$", message = "Invalid phone number")
    private String phoneNumber;

    @Schema(description = "User role", example = "USER")
    private Role role;

    @Schema(description = "Block/unblock the account", example = "false")
    private Boolean blocked;

    @Schema(description = "Allow deferred payment", example = "true")
    private Boolean canPayLater;

    /**
     * Individual markup percentage.
     *
     * <ul>
     *   <li>{@code null} (field absent from JSON) → do not change current value.</li>
     *   <li>{@code Optional.empty()} (JSON {@code null}) → clear individual markup;
     *       price engine will fall back to global configuration.</li>
     *   <li>{@code Optional.of(v)} → set to {@code v}.</li>
     * </ul>
     */
    @JsonDeserialize(using = NullableDecimalDeserializer.class)
    @Schema(
            description = "Individual markup percentage. Send null to clear (revert to global fallback).",
            example = "0.10",
            nullable = true
    )
    private Optional<@DecimalMin(value = "0.0", message = "Invalid markup percentage") BigDecimal> markupPercentage;

    @Schema(description = "UI localization preference", example = "RUS")
    private Localization localization;

    @Schema(description = "Profile data (surname, name, patronymic)")
    @Valid
    private Profile profile;
}
