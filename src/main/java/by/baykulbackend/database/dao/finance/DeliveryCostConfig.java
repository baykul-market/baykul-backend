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
@Table(name = "delivery_cost_config")
public class DeliveryCostConfig {
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
            description = "Minimum sum of the order",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "1000.01",
            minimum = "0.00"
    )
    @Column(name = "minimum_sum")
    @JsonView({Views.ConfigView.Get.class, Views.ConfigView.Post.class, Views.ConfigView.Patch.class})
    private BigDecimal minimumSum;

    @Schema(
            description = "Markup type",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "PERCENTAGE",
            allowableValues = {"PERCENTAGE", "SUM"}
    )
    @Column(name = "markup_type")
    @Enumerated(EnumType.STRING)
    @JsonView({Views.ConfigView.Get.class, Views.ConfigView.Post.class, Views.ConfigView.Patch.class})
    private DeliveryMarkupType markupType;

    @Schema(
            description = "Value of markup in system currency",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "0.1",
            minimum = "0"
    )
    @Column(name = "value")
    @JsonView({Views.ConfigView.Get.class, Views.ConfigView.Post.class, Views.ConfigView.Patch.class})
    private BigDecimal value;
}
