package by.baykulbackend.database.dto.finance;

import by.baykulbackend.database.dao.finance.DeliveryMarkupType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Schema(description = "Delivery cost configuration rule")
public class DeliveryCostConfigDto {
    
    @Schema(
            description = "Unique identifier",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private UUID id;

    @Schema(
            description = "Minimum order sum to apply this rule",
            example = "1000.00",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private BigDecimal minimumSum;

    @Schema(
            description = "Type of markup (PERCENTAGE or SUM)",
            example = "PERCENTAGE",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private DeliveryMarkupType markupType;

    @Schema(
            description = "Value of markup (percentage or fixed sum in system currency)",
            example = "0.10",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private BigDecimal value;
}