package by.baykulbackend.database.dto.balance;

import by.baykulbackend.database.dao.balance.BalanceOperationType;
import by.baykulbackend.database.dao.finance.Currency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(
        description = "Balance operation request",
        examples = {
                """
                {
                    "userId": "123e4567-e89b-12d3-a456-426614174001",
                    "amount": 120.00,
                    "currency": "RUB",
                    "operationType": "PAYMENT",
                    "description": "Payment of penalties"
                }
                """,
                """
                {
                    "balanceId": "123e4567-e89b-12d3-a456-426614174001",
                    "amount": 340.20,
                    "currency": "RUB",
                    "operationType": "REPLENISHMENT"
                }
                """
        }
)
public class BalanceOperationDto {

    @Schema(
            description = "User UUID for the operation. Can be used instead of balanceId. " +
                    "If userId is provided, the user's balance will be found.",
            example = "123e4567-e89b-12d3-a456-426614174001"
    )
    private String userId;

    @Schema(
            description = "Balance UUID for the operation. Can be used instead of userId.",
            example = "123e4567-e89b-12d3-a456-426614174001"
    )
    private String balanceId;

    @Schema(
            description = "Operation amount. Must be positive for all operation types.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minimum = "0.01",
            example = "120.00"
    )
    @NotNull
    private BigDecimal amount;

    @Schema(
            description = "Operation currency.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "RUB"
    )
    @NotNull
    private Currency currency;

    @Schema(
            description = "Operation type.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"REPLENISHMENT", "WITHDRAWAL", "PAYMENT"},
            example = "PAYMENT"
    )
    @NotNull
    private BalanceOperationType operationType;

    @Schema(
            description = "Operation description.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            example = "Penalty payment",
            maxLength = 255
    )
    private String description;
}
