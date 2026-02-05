package by.baykulbackend.database.dao.product;

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
@Table(name = "parts")
@Schema(description = "Spare part entity representing products with their description data")
public class Part {
    @Schema(
            description = "Unique identifier",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "123e4567-e89b-12d3-a456-426614174000"
    )
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @JsonView({Views.PartView.Get.class, Views.PartView.Post.class, Views.PartView.Put.class})
    private UUID id;

    @Schema(
            description = "Timestamp when the product was created",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "2024-01-15T10:30:00"
    )
    @Column(name = "created_ts")
    @JsonView(Views.PartView.Get.class)
    @CreationTimestamp
    private LocalDateTime createdTs;

    @Schema(
            description = "Timestamp when the product was last updated",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "2024-01-15T10:30:00"
    )
    @Column(name = "updated_ts")
    @JsonView(Views.PartView.Get.class)
    @UpdateTimestamp
    private LocalDateTime updatedTs;

    @Schema(
            description = "Part's unique article",
            requiredMode = Schema.RequiredMode.REQUIRED,
            maxLength = 50,
            example = "2405947"
    )
    @Column(name = "article", unique = true, nullable = false, length = 50)
    @JsonView({Views.PartView.Get.class, Views.PartView.Post.class, Views.PartView.Put.class})
    private String article;

    @Schema(
            description = "Part's name",
            requiredMode = Schema.RequiredMode.REQUIRED,
            maxLength = 255,
            example = "Engine Oil LL01 5W30"
    )
    @Column(name = "name", nullable = false)
    @JsonView({Views.PartView.Get.class, Views.PartView.Post.class, Views.PartView.Put.class})
    private String name;

    @Schema(
            description = "Part's weight in kilograms",
            nullable = true,
            minimum = "0",
            example = "150.4"
    )
    @Column(name = "weight")
    @JsonView({Views.PartView.Get.class, Views.PartView.Post.class, Views.PartView.Put.class})
    private Double weight;

    // TODO: find out what this field is for
    @Schema(
            description = "min_count",
            minimum = "1",
            defaultValue = "1",
            example = "3"
    )
    @Column(name = "min_count", nullable = false)
    @JsonView({Views.PartView.Get.class, Views.PartView.Post.class, Views.PartView.Put.class})
    private Integer minCount;

    @Schema(
            description = "Quantity of these parts in the storage",
            nullable = true,
            minimum = "0",
            example = "5"
    )
    @Column(name = "storage_count")
    @JsonView({Views.PartView.Get.class, Views.PartView.Post.class, Views.PartView.Put.class})
    private Integer storageCount;

    // TODO: find out what this field is for
    @Schema(
            description = "return_part",
            minimum = "0.00",
            defaultValue = "0.00",
            nullable = true,
            example = "3.01"
    )
    @Column(name = "return_part")
    @JsonView({Views.PartView.Get.class, Views.PartView.Post.class, Views.PartView.Put.class})
    private BigDecimal returnPart;

    @Schema(
            description = "Part's price value",
            minimum = "0.00",
            defaultValue = "0.00",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "7862.43"
    )
    @Column(name = "price", nullable = false)
    @JsonView({Views.PartView.Get.class, Views.PartView.Post.class, Views.PartView.Put.class})
    private BigDecimal price;

    @Schema(
            description = "Part's price currency",
            requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"EUR", "USD", "RUB"},
            defaultValue = "EUR",
            example = "EUR"
    )
    @Column(name = "currency", nullable = false)
    @Enumerated(EnumType.STRING)
    @JsonView({Views.PartView.Get.class, Views.PartView.Post.class, Views.PartView.Put.class})
    private Currency currency;

    @Schema(
            description = "Part's brand name",
            requiredMode = Schema.RequiredMode.REQUIRED,
            maxLength = 50,
            example = "rolls royce"
    )
    @Column(name = "brand", nullable = false, length = 50)
    @JsonView({Views.PartView.Get.class, Views.PartView.Post.class, Views.PartView.Put.class})
    private String brand;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Part part = (Part) o;

        return Objects.equals(id, part.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}