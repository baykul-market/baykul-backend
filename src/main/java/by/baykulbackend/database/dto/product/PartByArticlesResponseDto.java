package by.baykulbackend.database.dto.product;

import by.baykulbackend.database.dto.security.Views;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response DTO for parts search by multiple articles")
public class PartByArticlesResponseDto {

    @Schema(description = "List of parts found matching the provided articles")
    @JsonView(Views.PartView.Get.class)
    private List<PartDto> parts;
}
