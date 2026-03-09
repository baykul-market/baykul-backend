package by.baykulbackend.database.dao.balance;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Balance operation type enum")
public enum BalanceOperationType {
    REPLENISHMENT, WITHDRAWAL, PAYMENT
}