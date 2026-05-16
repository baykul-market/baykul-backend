package by.baykulbackend.database.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkippedRow {
    private int rowNumber;
    private String errorMessage;
    private String rawData;
}
