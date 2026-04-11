package by.baykulbackend.database.dao.bill;

import by.baykulbackend.database.dao.order.OrderProduct;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
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
@Table(name = "bills")
@Schema(description = "Bill entity representing bill of boxes")
public class Bill {
    @Schema(
            description = "Unique identifier",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "123e4567-e89b-12d3-a456-426614174000"
    )
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @JsonView({Views.BillView.Get.class, Views.BillView.Patch.class})
    private UUID id;

    @Schema(
            description = "Timestamp when the bill was created",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "2024-01-15T10:30:00"
    )
    @Column(name = "created_ts")
    @JsonView(Views.BillView.Get.class)
    @CreationTimestamp
    private LocalDateTime createdTs;

    @Schema(
            description = "Timestamp when the bill was last updated",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "2024-01-15T10:30:00"
    )
    @Column(name = "updated_ts")
    @JsonView(Views.BillView.Get.class)
    @UpdateTimestamp
    private LocalDateTime updatedTs;

    @Schema(
            description = "Unique number of the bill",
            example = "12345",
            minLength = 5,
            minimum = "10000"
    )
    @Column(name = "number", nullable = false, unique = true, updatable = false)
    @JsonView({Views.BillView.Get.class})
    private Long number;

    @Schema(
            description = "Bill status",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "DRAFT",
            allowableValues = {"DRAFT", "APPLIED"}
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @JsonView({Views.BillView.Get.class, Views.BillView.Patch.class})
    private BillStatus status;

    @Schema(
            description = "List of order products associated with this bill",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    @OneToMany(mappedBy = "bill", fetch = FetchType.LAZY)
    @JsonView({Views.BillFullView.class, Views.BillView.Post.class})
    private List<OrderProduct> orderProducts;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Bill bill = (Bill) o;

        return Objects.equals(id, bill.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
