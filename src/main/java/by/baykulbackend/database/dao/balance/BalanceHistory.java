package by.baykulbackend.database.dao.balance;

import by.baykulbackend.database.dao.finance.Currency;
import by.baykulbackend.database.dto.security.Views;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Data
@Entity
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id")
@Table(name = "balance_history")
@Schema(description = "Balance history entity representing balance sheet transaction history")
public class BalanceHistory {
    @Schema(
            description = "Unique identifier",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "123e4567-e89b-12d3-a456-426614174000"
    )
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @JsonView({Views.BalanceHistoryView.Get.class, Views.BalanceHistoryView.Post.class, Views.BalanceHistoryView.Patch.class})
    private UUID id;

    @Schema(
            description = "Timestamp when the balance history was created",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "2024-01-15T10:30:00"
    )
    @Column(name = "created_ts")
    @JsonView(Views.BalanceHistoryView.Get.class)
    @CreationTimestamp
    private LocalDateTime createdTs;

    @Schema(
            description = "Timestamp when the balance history was last updated",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "2024-01-15T10:30:00"
    )
    @Column(name = "updated_ts")
    @JsonView(Views.BalanceHistoryView.Get.class)
    @UpdateTimestamp
    private LocalDateTime updatedTs;

    @Schema(
            description = "Balance associated with this balance history",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "{\"id\": \"123e4567-e89b-12d3-a456-426614174001\", \"value\": \"120.00\"}"
    )
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "balance_id", nullable = false, updatable = false)
    @JsonView(Views.BalanceHistoryFullView.class)
    private Balance balance;

    @Schema(
            description = "Transaction amount",
            accessMode = Schema.AccessMode.READ_ONLY,
            minimum = "0",
            example = "120.00"
    )
    @Column(name = "amount")
    @JsonView(Views.BalanceHistoryView.Get.class)
    private BigDecimal amount;

    @Schema(
            description = "Transaction amount",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "RUB"
    )
    @Column(name = "currency", nullable = false)
    @Enumerated(EnumType.STRING)
    @JsonView(Views.BalanceHistoryView.Get.class)
    private Currency currency;

    @Schema(
            description = "Operation type",
            allowableValues = {"REPLENISHMENT", "WITHDRAWAL", "PAYMENT"},
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "PAYMENT"
    )
    @Column(name = "operation_type", nullable = false)
    @Enumerated(EnumType.STRING)
    @JsonView(Views.BalanceHistoryView.Get.class)
    private BalanceOperationType operationType;

    @Schema(
            description = "Result balance after performing operation",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "200.00"
    )
    @Column(name = "result_account", nullable = false)
    @JsonView(Views.BalanceHistoryView.Get.class)
    private BigDecimal resultAccount;

    @Schema(
            description = "Result balance currency after performing operation",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "RUB"
    )
    @Column(name = "result_currency", nullable = false)
    @Enumerated(EnumType.STRING)
    @JsonView(Views.BalanceHistoryView.Get.class)
    private Currency resultCurrency;

    @Schema(
            description = "Description of operation",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "Payment of penalties"
    )
    @Column(name = "description")
    @JsonView(Views.BalanceHistoryView.Get.class)
    private String description;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BalanceHistory balanceHistory = (BalanceHistory) o;

        return Objects.equals(id, balanceHistory.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
