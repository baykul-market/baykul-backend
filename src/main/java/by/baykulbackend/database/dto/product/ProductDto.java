package by.baykulbackend.database.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@Schema(description = "Data transfer object for CSV file upload containing product data")
public class ProductDto {

    @Schema(
            description = "CSV file with parts data. Must be in UTF-8 encoding with semicolon (;) separator.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            type = "string",
            format = "binary"
    )
    private MultipartFile csvFile;
}