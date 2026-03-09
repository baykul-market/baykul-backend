package by.baykulbackend.database.dao.finance;

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
import java.util.UUID;

@Data
@Entity
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id")
@Table(name = "price_config")
public class PriceConfig {
    @Schema(
            description = "Unique identifier",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "123e4567-e89b-12d3-a456-426614174000"
    )
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @JsonView({Views.PriceConfigView.Get.class, Views.PriceConfigView.Post.class, Views.PriceConfigView.Patch.class})
    private UUID id;

    @Schema(
            description = "Timestamp when the config was created",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "2024-01-15T10:30:00"
    )
    @Column(name = "created_ts")
    @JsonView(Views.PriceConfigView.Get.class)
    @CreationTimestamp
    private LocalDateTime createdTs;

    @Schema(
            description = "Timestamp when the config was last updated",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "2024-01-15T10:30:00"
    )
    @Column(name = "updated_ts")
    @JsonView(Views.PriceConfigView.Get.class)
    @UpdateTimestamp
    private LocalDateTime updatedTs;

    @Schema(
            description = "Default delivery percentage for all deliveries",
            example = "0.10",
            defaultValue = "0.10",
            minimum = "0"
    )
    @Column(name = "delivery_percentage", nullable = false)
    @JsonView({Views.PriceConfigView.Get.class, Views.PriceConfigView.Post.class, Views.PriceConfigView.Patch.class})
    private BigDecimal deliveryPercentage;

    @Schema(
            description = "Default markup percentage for user purchases",
            example = "0.10",
            defaultValue = "0.10",
            minimum = "0"
    )
    @Column(name = "markup_percentage", nullable = false)
    @JsonView({Views.PriceConfigView.Get.class, Views.PriceConfigView.Post.class, Views.PriceConfigView.Patch.class})
    private BigDecimal markupPercentage;

    @Schema(
            description = "Default currency for all purchases",
            example = "RUB",
            defaultValue = "RUB",
            enumAsRef = true
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false)
    @JsonView({Views.PriceConfigView.Get.class, Views.PriceConfigView.Post.class, Views.PriceConfigView.Patch.class})
    private Currency currency;
}
