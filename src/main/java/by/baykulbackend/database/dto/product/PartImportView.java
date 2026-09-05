package by.baykulbackend.database.dto.product;

import java.time.LocalDateTime;
import java.util.UUID;

public record PartImportView(UUID id, UUID sourceId, String filename, String uploadedBy, String status,
                             long sourceVersion, long totalRows, long validRows, long skipped, long duplicates,
                             long added, long updated, long removed, String errorMessage,
                             LocalDateTime createdTs, LocalDateTime updatedTs) { }
