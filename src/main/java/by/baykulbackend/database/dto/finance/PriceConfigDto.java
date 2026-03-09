package by.baykulbackend.database.dto.finance;

import by.baykulbackend.database.dao.finance.Currency;
import by.baykulbackend.database.dto.security.Views;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Price configuration data transfer object")
public class PriceConfigDto {

    @Schema(
            description = "Default delivery percentage for all deliveries",
            example = "0.10",
            defaultValue = "0.10",
            minimum = "0"
    )
    @JsonView({Views.PriceConfigView.Get.class, Views.PriceConfigView.Post.class, Views.PriceConfigView.Patch.class})
    private BigDecimal deliveryPercentage;

    @Schema(
            description = "Default markup percentage for user purchases",
            example = "0.10",
            defaultValue = "0.10",
            minimum = "0"
    )
    @JsonView({Views.PriceConfigView.Get.class, Views.PriceConfigView.Post.class, Views.PriceConfigView.Patch.class})
    private BigDecimal markupPercentage;

    @Schema(
            description = "Default currency for all purchases",
            example = "RUB",
            defaultValue = "RUB",
            enumAsRef = true
    )
    @JsonView({Views.PriceConfigView.Get.class, Views.PriceConfigView.Post.class, Views.PriceConfigView.Patch.class})
    private Currency currency;
}