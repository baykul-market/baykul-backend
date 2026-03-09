package by.baykulbackend.database.dao.balance;

import by.baykulbackend.database.dao.user.User;
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
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Data
@Entity
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id")
@Table(name = "balance")
@Schema(description = "Balance entity representing balance of each user")
public class Balance {
    @Schema(
            description = "Unique identifier",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "123e4567-e89b-12d3-a456-426614174000"
    )
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @JsonView({Views.BalanceView.Get.class, Views.BalanceView.Post.class, Views.BalanceView.Patch.class})
    private UUID id;

    @Schema(
            description = "Timestamp when the balance was created",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "2024-01-15T10:30:00"
    )
    @Column(name = "created_ts")
    @JsonView(Views.BalanceView.Get.class)
    @CreationTimestamp
    private LocalDateTime createdTs;

    @Schema(
            description = "Timestamp when the balance was last updated",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "2024-01-15T10:30:00"
    )
    @Column(name = "updated_ts")
    @JsonView(Views.BalanceView.Get.class)
    @UpdateTimestamp
    private LocalDateTime updatedTs;

    @Schema(
            description = "User associated with this balance",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "{\"id\": \"123e4567-e89b-12d3-a456-426614174001\", \"login\": \"john_doe\"}"
    )
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    @JsonView(Views.BalanceFullView.class)
    private User user;

    @Schema(
            description = "Balance account",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "1200.84"
    )
    @Column(name = "account", nullable = false)
    @JsonView({Views.BalanceView.Get.class})
    private BigDecimal account;

    @Schema(
            description = "List of balance operations history associated with that balance",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @OneToMany(mappedBy = "balance", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonView(Views.BalanceFullView.class)
    private List<BalanceHistory> balanceHistoryList;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Balance balance = (Balance) o;

        return Objects.equals(id, balance.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
