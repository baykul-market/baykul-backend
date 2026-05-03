package by.baykulbackend.database.dao.finance;

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
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
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
    @JsonView({Views.ConfigView.Get.class, Views.ConfigView.Post.class, Views.ConfigView.Patch.class})
    private UUID id;

    @Schema(
            description = "Timestamp when the config was created",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "2024-01-15T10:30:00"
    )
    @Column(name = "created_ts")
    @JsonView(Views.ConfigView.Get.class)
    @CreationTimestamp
    private LocalDateTime createdTs;

    @Schema(
            description = "Timestamp when the config was last updated",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "2024-01-15T10:30:00"
    )
    @Column(name = "updated_ts")
    @JsonView(Views.ConfigView.Get.class)
    @UpdateTimestamp
    private LocalDateTime updatedTs;

    @Schema(
            description = "Default markup percentage for user purchases",
            example = "0.10",
            defaultValue = "0.10",
            minimum = "0"
    )
    @Column(name = "markup_percentage", nullable = false)
    @JsonView({Views.ConfigView.Get.class, Views.ConfigView.Post.class, Views.ConfigView.Patch.class})
    private BigDecimal markupPercentage;

    @Schema(
            description = "Default currency for all purchases",
            example = "RUB",
            defaultValue = "RUB",
            enumAsRef = true
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false)
    @JsonView({Views.ConfigView.Get.class, Views.ConfigView.Post.class, Views.ConfigView.Patch.class})
    private Currency currency;
    @Schema(
            description = "Currency for which delivery rules are calculated",
            example = "EUR",
            enumAsRef = true
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_currency")
    @JsonView({Views.ConfigView.Get.class, Views.ConfigView.Post.class, Views.ConfigView.Patch.class})
    private Currency deliveryCurrency;

    @Schema(
            description = "Scale for rounding prices (e.g. 2 for pennies, 0 for units, -2 for hundreds)",
            example = "-2"
    )
    @Column(name = "rounding_scale")
    @JsonView({Views.ConfigView.Get.class, Views.ConfigView.Post.class, Views.ConfigView.Patch.class})
    private Integer roundingScale;

    @Schema(
            description = "Mathematical rounding mode",
            example = "CEILING",
            enumAsRef = true
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "rounding_mode")
    @JsonView({Views.ConfigView.Get.class, Views.ConfigView.Post.class, Views.ConfigView.Patch.class})
    private java.math.RoundingMode roundingMode;
}
