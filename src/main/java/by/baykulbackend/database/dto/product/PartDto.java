package by.baykulbackend.database.dto.product;

import by.baykulbackend.database.dao.finance.Currency;
import com.fasterxml.jackson.annotation.JsonView;
import by.baykulbackend.database.dto.security.Views;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@Schema(description = "Part data transfer object")
public class PartDto {

    @Schema(description = "Unique identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    @JsonView(Views.PartView.Get.class)
    private UUID id;

    @Schema(description = "Timestamp when the product was created", example = "2024-01-15T10:30:00")
    @JsonView(Views.PartView.Get.class)
    private LocalDateTime createdTs;

    @Schema(description = "Timestamp when the product was last updated", example = "2024-01-15T10:30:00")
    @JsonView(Views.PartView.Get.class)
    private LocalDateTime updatedTs;

    @Schema(description = "Part's unique article", example = "2405947")
    @JsonView(Views.PartView.Get.class)
    private String article;

    @Schema(description = "Part's name", example = "Engine Oil LL01 5W30")
    @JsonView(Views.PartView.Get.class)
    private String name;

    @Schema(description = "Part's weight in kilograms", example = "150.4")
    @JsonView(Views.PartView.Get.class)
    private Double weight;

    @Schema(description = "Minimum order count", example = "3")
    @JsonView(Views.PartView.Get.class)
    private Integer minCount;

    @Schema(description = "Quantity in storage", example = "5")
    @JsonView(Views.PartView.Get.class)
    private Integer storageCount;

    @Schema(description = "Return part value", example = "3.01")
    @JsonView(Views.PartView.Get.class)
    private BigDecimal returnPart;

    @Schema(description = "Part's price", example = "3.00")
    @JsonView(Views.PartView.Get.class)
    private BigDecimal price;

    @Schema(description = "Part's price currency", example = "EUR")
    @JsonView(Views.PartView.Get.class)
    private Currency currency;

    @Schema(description = "Part's brand name", example = "rolls royce")
    @JsonView(Views.PartView.Get.class)
    private String brand;
}