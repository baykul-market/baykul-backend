package by.baykulbackend.database.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "Data transfer object for creating/updating currency exchange rates")
public class CurrencyExchangeDto {
    @Schema(
            description = "Source currency code (ISO 4217)",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "USD",
            maxLength = 3,
            minLength = 3
    )
    @NotNull
    private String currencyFrom;

    @Schema(
            description = "Target currency code (ISO 4217)",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "EUR",
            maxLength = 3,
            minLength = 3
    )
    @NotNull
    private String currencyTo;

    @Schema(
            description = "Exchange rate from source to target currency",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "0.85"
    )
    @NotNull
    private BigDecimal rate;

    @Schema(
            description = "Flag indicating whether to create exchange rate in both directions. " +
                    "If true, creates both FROM->TO and TO->FROM rates (TO->FROM rate is calculated as 1/rate)",
            defaultValue = "false",
            example = "false"
    )
    private Boolean bothDirections;

    @Schema(
            description = "Flag indicating whether to replace existing exchange rates. " +
                    "If true, overwrites existing rates. If false, returns CONFLICT if rates already exist",
            defaultValue = "false",
            example = "false"
    )
    private Boolean replaceExisting;
}