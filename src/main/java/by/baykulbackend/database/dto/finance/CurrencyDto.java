package by.baykulbackend.database.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Data transfer object for currency information")
public class CurrencyDto {
    @NotNull
    @Schema(
            description = "Currency code (ISO 4217)",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "USD"
    )
    private String code;

    @NotNull
    @Schema(
            description = "Currency name in Russian",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "Доллар США"
    )
    private String russianName;

    @NotNull
    @Schema(
            description = "Countries where this currency is used",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "США, Эквадор, Сальвадор, Панама и др."
    )
    private String countries;
}