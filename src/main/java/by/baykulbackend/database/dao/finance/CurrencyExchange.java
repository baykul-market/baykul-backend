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
@Table(name = "currency_exchange")
@Schema(description = "Currency exchange entity representing exchange rates between two currencies")
public class CurrencyExchange {
    @Schema(
            description = "Unique identifier",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "123e4567-e89b-12d3-a456-426614174000"
    )
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @JsonView({
            Views.CurrencyExchangeView.Get.class,
            Views.CurrencyExchangeView.Post.class,
            Views.CurrencyExchangeView.Patch.class
    })
    private UUID id;

    @Schema(
            description = "Timestamp when the currency exchange was created",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "2024-01-15T10:30:00"
    )
    @Column(name = "created_ts")
    @JsonView(Views.CurrencyExchangeView.Get.class)
    @CreationTimestamp
    private LocalDateTime createdTs;

    @Schema(
            description = "Timestamp when the currency exchange was last updated",
            accessMode = Schema.AccessMode.READ_ONLY,
            example = "2024-01-15T10:30:00"
    )
    @Column(name = "updated_ts")
    @JsonView(Views.CurrencyExchangeView.Get.class)
    @UpdateTimestamp
    private LocalDateTime updatedTs;

    @Schema(
            description = "Source currency code (ISO 4217)",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "USD"
    )
    @Column(name = "currency_from", nullable = false, length = 3)
    @Enumerated(EnumType.STRING)
    @JsonView({Views.CurrencyExchangeView.Get.class, Views.CurrencyExchangeView.Post.class})
    private Currency currencyFrom;

    @Schema(
            description = "Target currency code (ISO 4217)",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "EUR"
    )
    @Column(name = "currency_to", nullable = false, length = 3)
    @Enumerated(EnumType.STRING)
    @JsonView({Views.CurrencyExchangeView.Get.class, Views.CurrencyExchangeView.Post.class})
    private Currency currencyTo;

    @Schema(
            description = "Exchange rate from source to target currency",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "0.85"
    )
    @Column(name = "rate", nullable = false)
    @JsonView({
            Views.CurrencyExchangeView.Get.class,
            Views.CurrencyExchangeView.Post.class,
            Views.CurrencyExchangeView.Patch.class
    })
    private BigDecimal rate;
}