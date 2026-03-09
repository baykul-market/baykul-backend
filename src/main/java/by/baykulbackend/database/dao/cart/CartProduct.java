package by.baykulbackend.database.dao.cart;

import by.baykulbackend.database.dao.product.Part;
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
import java.util.Objects;
import java.util.UUID;

@Data
@Entity
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id")
@Table(name = "cart_products")
@Schema(description = "Cart product entity representing cart item")
public class CartProduct {
    @Schema(
            description = "Unique identifier",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "123e4567-e89b-12d3-a456-426614174000"
    )
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @JsonView({Views.CartProductView.Get.class, Views.CartProductView.Post.class, Views.CartProductView.Patch.class})
    private UUID id;

    @Schema(
            description = "Timestamp when the cart product was created",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "2024-01-15T10:30:00"
    )
    @Column(name = "created_ts")
    @JsonView(Views.CartProductView.Get.class)
    @CreationTimestamp
    private LocalDateTime createdTs;

    @Schema(
            description = "Timestamp when the cart product was last updated",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "2024-01-15T10:30:00"
    )
    @Column(name = "updated_ts")
    @JsonView(Views.CartProductView.Get.class)
    @UpdateTimestamp
    private LocalDateTime updatedTs;

    @Schema(
            description = "Cart associated with this cart product",
            example = "{\"id\": \"123e4567-e89b-12d3-a456-426614174001\"}",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false, updatable = false)
    @JsonView(Views.CartProductFullView.class)
    private Cart cart;

    @Schema(
            description = "Part added to the cart",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "{\"id\": \"123e4567-e89b-12d3-a456-426614174001\", \"article\": \"exampleArticle123\"}"
    )
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "part_id", nullable = false, updatable = false)
    @JsonView({Views.CartProductFullView.class, Views.CartFullView.class, Views.CartProductView.Post.class})
    private Part part;

    @Schema(
            description = "Count of parts in the cart",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minimum = "1",
            example = "3"
    )
    @Column(name = "parts_count", nullable = false)
    @JsonView({Views.CartProductView.Get.class, Views.CartProductView.Post.class, Views.CartProductView.Patch.class})
    private Integer partsCount;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CartProduct cartProduct = (CartProduct) o;

        return Objects.equals(id, cartProduct.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
