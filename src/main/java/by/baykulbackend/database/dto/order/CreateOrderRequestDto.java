package by.baykulbackend.database.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request object for creating an order")
public class CreateOrderRequestDto {
    @Schema(description = "If true, order is created with CREATED status and no payment is deducted immediately.", defaultValue = "false")
    private boolean payLater = false;
}
