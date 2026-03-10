package by.baykulbackend.database.dao.finance;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Delivery cost type enum")
public enum DeliveryMarkupType {
    PERCENTAGE, SUM
}
