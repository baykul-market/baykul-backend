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
@Schema(description = "Request DTO for searching parts by multiple articles")
public class PartByArticlesRequestDto {

    @Schema(
            description = "List of direct article numbers to search for",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "[\"2405947\", \"2405948\"]"
    )
    @JsonView(Views.PartView.Get.class)
    private List<String> articles;
}
