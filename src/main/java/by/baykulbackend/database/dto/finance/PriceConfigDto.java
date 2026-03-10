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
            description = "Delivery cost configuration rules"
    )
    private List<DeliveryCostConfigDto> deliveryCostConfigs;
}