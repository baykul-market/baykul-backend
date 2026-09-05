package by.baykulbackend.database.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsvUploadResult {
    private java.util.UUID importId;
    private java.util.UUID sourceId;
    private boolean skippedDetailsTruncated;
    private int saved;
    private int updated;
    private int skipped;
    @Builder.Default
    private List<SkippedRow> skippedDetails = new ArrayList<>();
}
