package by.baykulbackend.database.dao.order;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Order status enum")
public enum OrderStatus {
    CONFIRMATION_WAITING,
    PAYMENT_WAITING,
    ORDERED,
    ON_WAY,
    IN_WAREHOUSE,
    READY_FOR_PICKUP,
    COMPLETED,
    CANCELLED
}