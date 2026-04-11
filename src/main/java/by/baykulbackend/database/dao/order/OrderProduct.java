package by.baykulbackend.database.dao.order;

import by.baykulbackend.database.dao.bill.Bill;
import by.baykulbackend.database.dao.finance.Currency;
import by.baykulbackend.database.dao.product.Part;
import by.baykulbackend.database.dto.security.Views;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id")
@Table(name = "order_products")
@Schema(description = "Order product entity representing order item")
public class OrderProduct {
    @Schema(
            description = "Unique identifier",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "123e4567-e89b-12d3-a456-426614174000"
    )
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @JsonView({Views.OrderProductView.Get.class, Views.OrderProductView.Post.class, Views.OrderProductView.Patch.class})
    private UUID id;

    @Schema(
            description = "Timestamp when the order product was created",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "2024-01-15T10:30:00"
    )
    @Column(name = "created_ts")
    @JsonView(Views.OrderProductView.Get.class)
    @CreationTimestamp
    private LocalDateTime createdTs;

    @Schema(
            description = "Timestamp when the order product was last updated",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "2024-01-15T10:30:00"
    )
    @Column(name = "updated_ts")
    @JsonView(Views.OrderProductView.Get.class)
    @UpdateTimestamp
    private LocalDateTime updatedTs;

    @Schema(
            description = "Unique number of the order product",
            example = "123456",
            minLength = 6,
            minimum = "100000"
    )
    @Column(name = "number", unique = true)
    @JsonView({Views.OrderProductView.Get.class, Views.OrderProductView.Post.class, Views.OrderProductView.Patch.class})
    private Long number;

    @Schema(
            description = "Order product status",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "ORDERED",
            allowableValues = {"ORDERED", "IN_WAREHOUSE", "ON_WAY", "ARRIVED", "DELIVERED", "RETURNED", "CANCELLED"}
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @JsonView({Views.OrderView.Get.class, Views.OrderView.Post.class, Views.OrderView.Patch.class})
    private BoxStatus status;

    @Schema(
            description = "Order associated with this order product",
            example = "{\"id\": \"123e4567-e89b-12d3-a456-426614174001\"}",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    @JsonView(Views.OrderProductFullView.class)
    private Order order;

    @Schema(
            description = "Part added to the order",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "{\"id\": \"123e4567-e89b-12d3-a456-426614174001\", \"article\": \"exampleArticle123\"}"
    )
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "part_id", nullable = false, updatable = false)
    @JsonView({Views.OrderProductFullView.class, Views.OrderFullView.class, Views.OrderProductView.Post.class})
    private Part part;

    @Schema(
            description = "Count of parts in the order",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minimum = "1",
            example = "3"
    )
    @Column(name = "parts_count", nullable = false)
    @JsonView({Views.OrderProductView.Get.class, Views.OrderProductView.Post.class, Views.OrderProductView.Patch.class})
    private Integer partsCount;

    @Schema(
            description = "Price value captured at time of order addition",
            requiredMode = Schema.RequiredMode.REQUIRED,
            accessMode = Schema.AccessMode.READ_ONLY,
            minimum = "0.00",
            example = "16.00"
    )
    @Column(name = "price", nullable = false)
    @JsonView(Views.OrderProductView.Get.class)
    private BigDecimal price;

    @Schema(
            description = "Price currency captured at time of order addition",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "RUB"
    )
    @Column(name = "currency", nullable = false)
    @Enumerated(EnumType.STRING)
    @JsonView(Views.OrderProductView.Get.class)
    private Currency currency;

    @Schema(
            description = "Bill associated with this order product",
            example = "{\"id\": \"123e4567-e89b-12d3-a456-426614174001\"}",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id")
    @JsonView(Views.OrderProductFullView.class)
    private Bill bill;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        OrderProduct orderProduct = (OrderProduct) o;

        return Objects.equals(id, orderProduct.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
