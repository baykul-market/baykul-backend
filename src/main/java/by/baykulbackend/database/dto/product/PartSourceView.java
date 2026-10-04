package by.baykulbackend.database.dto.product;

import by.baykulbackend.database.dao.product.PartSource;
import java.time.LocalDateTime;
import java.util.UUID;

public record PartSourceView(UUID id, String name, PartSource.Status status, boolean systemSource, long version,
                             long partsCount, PartImportView lastImport, LocalDateTime createdTs, LocalDateTime updatedTs) { }
