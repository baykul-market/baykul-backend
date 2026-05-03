package by.baykulbackend.database.dto.finance;

import by.baykulbackend.database.dao.finance.Currency;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "Price configuration data transfer object")
public class PriceConfigDto {

    @Schema(
            description = "Default markup percentage for user purchases",
            example = "0.10",
            minimum = "0"
    )
    private BigDecimal markupPercentage;

    @Schema(
            description = "Default system currency",
            example = "RUB"
    )
    private Currency systemCurrency;

    @Schema(
            description = "Currency for which delivery rules are calculated",
            example = "EUR"
    )
    private Currency deliveryCurrency;

    @Schema(
            description = "Scale for rounding prices (e.g. 2 for pennies, 0 for units, -2 for hundreds)",
            example = "-2"
    )
    private Integer roundingScale;

    @Schema(
            description = "Mathematical rounding mode",
            example = "CEILING"
    )
    private java.math.RoundingMode roundingMode;

    @Schema(
            description = "Delivery cost configuration rules"
    )
    private List<DeliveryCostConfigDto> deliveryCostConfigs;
}