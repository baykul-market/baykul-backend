package by.baykulbackend.database.dao.order;

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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Data
@Entity
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id")
@Table(name = "orders")
@Schema(description = "Order entity representing orders of each user")
public class Order {
    @Schema(
            description = "Unique identifier",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "123e4567-e89b-12d3-a456-426614174000"
    )
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @JsonView({Views.OrderView.Get.class, Views.OrderView.Post.class, Views.OrderView.Put.class})
    private UUID id;

    @Schema(
            description = "Timestamp when the order was created",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "2024-01-15T10:30:00"
    )
    @Column(name = "created_ts")
    @JsonView(Views.OrderView.Get.class)
    @CreationTimestamp
    private LocalDateTime createdTs;

    @Schema(
            description = "Timestamp when the order was last updated",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "2024-01-15T10:30:00"
    )
    @Column(name = "updated_ts")
    @JsonView(Views.OrderView.Get.class)
    @UpdateTimestamp
    private LocalDateTime updatedTs;

    @Schema(
            description = "Unique number of the order",
            example = "123456",
            minLength = 6,
            minimum = "100000"
    )
    @Column(name = "number", nullable = false, unique = true, updatable = false)
    @JsonView({Views.OrderView.Get.class, Views.OrderView.Post.class, Views.OrderView.Put.class})
    private Long number;

    @Schema(
            description = "Order status",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "CREATED",
            allowableValues = {"CREATED", "PAID", "PROCESSING", "COMPLETED", "CANCELED"}
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @JsonView({Views.OrderView.Get.class, Views.OrderView.Post.class, Views.OrderView.Put.class})
    private OrderStatus status;

    @Schema(
            description = "User associated with this order",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "{\"id\": \"123e4567-e89b-12d3-a456-426614174001\", \"login\": \"john_doe\"}"
    )
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    @JsonView(Views.OrderFullView.class)
    private User user;

    @Schema(
            description = "List of order products associated with that order",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonView(Views.OrderFullView.class)
    private List<OrderProduct> orderProducts;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Order order = (Order) o;

        return Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
