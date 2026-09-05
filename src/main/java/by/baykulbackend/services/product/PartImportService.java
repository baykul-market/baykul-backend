package by.baykulbackend.services.product;

import by.baykulbackend.database.dao.product.PartSource;
import by.baykulbackend.database.dto.product.CsvUploadResult;
import by.baykulbackend.database.dto.product.PartImportView;
import by.baykulbackend.exceptions.CatalogConflictException;
import by.baykulbackend.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

@lombok.extern.slf4j.Slf4j
@Service
public class PartImportService {
    public static final long MAX_BYTES = 200L * 1024 * 1024;
    private static final int CHUNK_SIZE = 5000;
    private final JdbcTemplate jdbc;
    private final PartImportStore store;
    private final TransactionTemplate transaction;
    private final ThreadPoolTaskExecutor executor;
    private final Path directory;

    public PartImportService(JdbcTemplate jdbc, PartImportStore store, PlatformTransactionManager manager,
                             @Qualifier("partImportExecutor") ThreadPoolTaskExecutor executor,
                             @Value("${app.imports.directory:uploads/imports}") String directory) {
        this.jdbc = jdbc;
        this.store = store;
        this.transaction = new TransactionTemplate(manager);
        this.executor = executor;
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    public PartImportView upload(UUID sourceId, MultipartFile file, String username) {
        UUID id = receive(sourceId, file, username);
        dispatch(id, () -> process(id));
        return store.get(id);
    }

    private UUID receive(UUID sourceId, MultipartFile file, String username) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Select a nonempty CSV/TXT file");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new MaxUploadSizeExceededException(MAX_BYTES);
        }
        UUID id = UUID.randomUUID();
        String filename = file.getOriginalFilename() == null ? "upload.csv" : file.getOriginalFilename();
        filename = filename.replace('\\', '/');
        filename = filename.substring(filename.lastIndexOf('/') + 1);
        if (filename.isBlank() || filename.length() > 255) {
            throw new IllegalArgumentException("Filename must contain 1 to 255 characters");
        }
        String originalName = filename;
        try {
            Files.createDirectories(directory);
            try (var input = file.getInputStream(); var output = Files.newOutputStream(filePath(id))) {
                long copied = input.transferTo(output);
                if (copied > MAX_BYTES) {
                    throw new MaxUploadSizeExceededException(MAX_BYTES);
                }
            }
            transaction.executeWithoutResult(tx -> {
                Map<String, Object> source = lockSource(sourceId);
                if (Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM part_imports WHERE source_id = ? "
                        + "AND status IN ('QUEUED','PROCESSING','READY','APPLYING'))", Boolean.class, sourceId))) {
                    throw new CatalogConflictException("IMPORT_PENDING", "This source already has an unfinished import");
                }
                jdbc.update("INSERT INTO part_imports (id, source_id, filename, uploaded_by, status, source_version) "
                        + "VALUES (?, ?, ?, ?, 'QUEUED', ?)", id, sourceId, originalName, username, source.get("version"));
            });
            return id;
        } catch (IOException ex) {
            deleteFile(id);
            throw new IllegalStateException("Could not store upload", ex);
        } catch (RuntimeException ex) {
            deleteFile(id);
            throw ex;
        }
    }

    private Map<String, Object> lockSource(UUID sourceId) {
        List<Map<String, Object>> result = jdbc.queryForList("SELECT status, version FROM part_sources WHERE id = ? FOR UPDATE", sourceId);
        if (result.isEmpty()) {
            throw new NotFoundException("Source not found");
        }
        Map<String, Object> source = result.getFirst();
        if ("ARCHIVED".equals(source.get("status"))) {
            throw new CatalogConflictException("SOURCE_ARCHIVED", "Source is archived");
        }
        return source;
    }

    private void dispatch(UUID id, Runnable task) {
        try {
            executor.execute(task);
        } catch (RejectedExecutionException ex) {
            fail(id, "Import queue is full; please retry later");
            deleteFile(id);
        }
    }

    public void process(UUID id) {
        if (jdbc.update("UPDATE part_imports SET status = 'PROCESSING', updated_ts = CURRENT_TIMESTAMP WHERE id = ? AND status = 'QUEUED'", id) == 0) {
            deleteFile(id);
            return;
        }
        long[] counters = new long[3]; // total, valid, skipped; bounded independently of the number of rows
        try (var input = Files.newInputStream(filePath(id));
                var reader = new BoundedCsvReader(new InputStreamReader(input, StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)))) {
            BoundedCsvReader.Line header = reader.next();
            if (header == null || header.oversized() || header.text().split(";", -1).length != 8) {
                throw new IllegalArgumentException("Expected a header with 8 semicolon-separated columns");
            }
            List<Object[]> rows = new ArrayList<>(CHUNK_SIZE);
            List<Object[]> errors = new ArrayList<>(CHUNK_SIZE);
            BoundedCsvReader.Line line;
            while ((line = reader.next()) != null) {
                long rowNumber = ++counters[0] + 1;
                try {
                    if (line.oversized()) {
                        throw new IllegalArgumentException("Line exceeds 8192 characters");
                    }
                    PartCsvRow parsed = PartCsvRow.parse(line.text());
                    rows.add(new Object[]{id, parsed.article(), parsed.brand(), rowNumber, parsed.name(), parsed.weight(),
                            parsed.minCount(), parsed.storageCount(), parsed.returnPart(), parsed.price()});
                    counters[1]++;
                } catch (IllegalArgumentException ex) {
                    errors.add(new Object[]{id, rowNumber, ex.getMessage(), truncate(line.text(), 2000)});
                    counters[2]++;
                }
                if (rows.size() + errors.size() >= CHUNK_SIZE) {
                    if (!flush(id, rows, errors, counters)) {
                        return;
                    }
                }
            }
            if (!flush(id, rows, errors, counters)) {
                return;
            }
            if (counters[1] == 0) {
                throw new IllegalArgumentException("File has no valid data rows; the current catalog is unchanged");
            }
            refreshPreview(id, "PROCESSING");
        } catch (Exception ex) {
            log.warn("Import {} could not be processed", id, ex);
            fail(id, ex instanceof IllegalArgumentException ? ex.getMessage() : "File processing failed; the current catalog is unchanged");
        } finally {
            deleteFile(id);
        }
    }

    private boolean flush(UUID id, List<Object[]> rows, List<Object[]> errors, long[] counters) {
        boolean accepted = Boolean.TRUE.equals(transaction.execute(tx -> {
            if (!"PROCESSING".equals(store.lock(id).status())) {
                return false;
            }
            // PostgreSQL's reWriteBatchedInserts may turn a batch into a single INSERT. Its ON CONFLICT
            // clause cannot update the same key twice in that statement, so collapse keys within a chunk.
            Map<List<Object>, Object[]> distinctRows = new java.util.LinkedHashMap<>();
            rows.forEach(row -> distinctRows.put(List.of(row[1], row[2]), row));
            jdbc.batchUpdate("INSERT INTO part_import_rows (import_id, article, brand, row_number, name, weight, "
                    + "min_count, storage_count, return_part, price) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                    + "ON CONFLICT (import_id, article, brand) DO UPDATE SET row_number = EXCLUDED.row_number, name = EXCLUDED.name, "
                    + "weight = EXCLUDED.weight, min_count = EXCLUDED.min_count, storage_count = EXCLUDED.storage_count, "
                    + "return_part = EXCLUDED.return_part, price = EXCLUDED.price", new ArrayList<>(distinctRows.values()));
            jdbc.batchUpdate("INSERT INTO part_import_errors (import_id, row_number, error_message, raw_data) VALUES (?, ?, ?, ?)", errors);
            jdbc.update("UPDATE part_imports SET total_rows = ?, valid_rows = ?, skipped = ?, updated_ts = CURRENT_TIMESTAMP WHERE id = ?",
                    counters[0], counters[1], counters[2], id);
            return true;
        }));
        rows.clear();
        errors.clear();
        return accepted;
    }

    public PartImportView recheck(UUID sourceId, UUID id) {
        store.get(sourceId, id);
        refreshPreview(id, "READY");
        return store.get(id);
    }

    private void refreshPreview(UUID id, String expectedStatus) {
        transaction.executeWithoutResult(tx -> {
            PartImportView initial = store.get(id);
            Map<String, Object> source = lockSource(initial.sourceId());
            PartImportView job = store.lock(id);
            if (!expectedStatus.equals(job.status())) {
                throw new CatalogConflictException("IMPORT_STATE", "Import is not ready for this operation");
            }
            long unique = jdbc.queryForObject("SELECT count(*) FROM part_import_rows WHERE import_id = ?", Long.class, id);
            long matched = jdbc.queryForObject("SELECT count(*) FROM part_import_rows r JOIN parts p ON p.source_id = ? "
                    + "AND p.article = r.article AND p.brand = r.brand WHERE r.import_id = ?", Long.class, job.sourceId(), id);
            long removed = jdbc.queryForObject("SELECT count(*) FROM parts p WHERE p.source_id = ? AND p.catalog_present = true "
                    + "AND NOT EXISTS (SELECT 1 FROM part_import_rows r WHERE r.import_id = ? AND r.article = p.article AND r.brand = p.brand)",
                    Long.class, job.sourceId(), id);
            jdbc.update("UPDATE part_imports SET status = 'READY', source_version = ?, duplicates = ?, added = ?, updated = ?, "
                    + "removed = ?, error_message = NULL, accept_skipped_rows = false, updated_ts = CURRENT_TIMESTAMP WHERE id = ?",
                    source.get("version"), job.validRows() - unique, unique - matched, matched, removed, id);
        });
    }

    public PartImportView apply(UUID sourceId, UUID id, boolean acceptSkippedRows) {
        PartImportView current = store.get(sourceId, id);
        if ("COMPLETED".equals(current.status())) {
            return current;
        }
        boolean publish = Boolean.TRUE.equals(transaction.execute(tx -> {
            Map<String, Object> source = lockSource(sourceId);
            PartImportView job = store.lock(id);
            if (List.of("APPLYING", "COMPLETED").contains(job.status())) {
                return false;
            }
            requireReady(job, source, acceptSkippedRows);
            jdbc.update("UPDATE part_imports SET status = 'APPLYING', accept_skipped_rows = ?, updated_ts = CURRENT_TIMESTAMP WHERE id = ?",
                    acceptSkippedRows, id);
            return true;
        }));
        if (publish) {
            dispatch(id, () -> publish(id, true));
        }
        return store.get(id);
    }

    private void requireReady(PartImportView job, Map<String, Object> source, boolean acceptSkippedRows) {
        if (!"READY".equals(job.status()) || job.validRows() == 0) {
            throw new CatalogConflictException("IMPORT_STATE", "Import is not ready to apply");
        }
        if (job.sourceVersion() != ((Number) source.get("version")).longValue()) {
            throw new CatalogConflictException("SOURCE_CHANGED", "Source has changed; refresh the preview and confirm again");
        }
        if (job.skipped() > 0 && !acceptSkippedRows) {
            throw new CatalogConflictException("SKIPPED_ROWS_CONFIRMATION", "Confirm applying valid rows and removing missing or skipped offers");
        }
    }

    private void publish(UUID id, boolean replaceCatalog) {
        try {
            transaction.executeWithoutResult(tx -> {
                PartImportView initial = store.get(id);
                Map<String, Object> source = lockSource(initial.sourceId());
                PartImportView job = store.lock(id);
                if (!"APPLYING".equals(job.status())) {
                    return;
                }
                if (job.sourceVersion() != ((Number) source.get("version")).longValue()) {
                    throw new CatalogConflictException("SOURCE_CHANGED", "Source changed before publication; review the preview again");
                }
                jdbc.update("INSERT INTO parts (id, created_ts, updated_ts, source_id, catalog_present, article, brand, name, weight, "
                        + "min_count, storage_count, return_part, price, currency) "
                        + "SELECT gen_random_uuid(), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, true, article, brand, name, weight, "
                        + "min_count, storage_count, return_part, price, 'EUR' FROM part_import_rows WHERE import_id = ? "
                        + "ON CONFLICT (source_id, article, brand) DO UPDATE SET updated_ts = CURRENT_TIMESTAMP, catalog_present = true, "
                        + "name = EXCLUDED.name, weight = " + (replaceCatalog ? "EXCLUDED.weight" : "COALESCE(EXCLUDED.weight, parts.weight)")
                        + ", min_count = EXCLUDED.min_count, storage_count = "
                        + (replaceCatalog ? "EXCLUDED.storage_count" : "COALESCE(EXCLUDED.storage_count, parts.storage_count)") + ", "
                        + "return_part = EXCLUDED.return_part, price = EXCLUDED.price, currency = EXCLUDED.currency", job.sourceId(), id);
                if (replaceCatalog) {
                    jdbc.update("UPDATE parts p SET catalog_present = false, updated_ts = CURRENT_TIMESTAMP WHERE source_id = ? "
                            + "AND catalog_present = true AND NOT EXISTS (SELECT 1 FROM part_import_rows r "
                            + "WHERE r.import_id = ? AND r.article = p.article AND r.brand = p.brand)", job.sourceId(), id);
                }
                jdbc.update("UPDATE part_sources SET version = version + 1, updated_ts = CURRENT_TIMESTAMP WHERE id = ?", job.sourceId());
                jdbc.update("UPDATE part_imports SET status = 'COMPLETED', removed = ?, updated_ts = CURRENT_TIMESTAMP WHERE id = ?",
                        replaceCatalog ? job.removed() : 0, id);
                store.discardRows(id);
            });
        } catch (CatalogConflictException ex) {
            if ("SOURCE_CHANGED".equals(ex.getCode())) {
                refreshPreview(id, "APPLYING");
                jdbc.update("UPDATE part_imports SET error_message = ? WHERE id = ?", ex.getMessage(), id);
            } else {
                fail(id, ex.getMessage());
            }
        } catch (Exception ex) {
            log.error("Import {} publication rolled back", id, ex);
            fail(id, "Publication failed; the current catalog is unchanged");
        }
    }

    public PartImportView cancel(UUID sourceId, UUID id) {
        store.get(sourceId, id);
        transaction.executeWithoutResult(tx -> {
            PartImportView job = store.lock(id);
            if ("CANCELLED".equals(job.status())) {
                return;
            }
            if (!List.of("QUEUED", "PROCESSING", "READY").contains(job.status())) {
                throw new CatalogConflictException("IMPORT_STATE", "This import can no longer be cancelled");
            }
            jdbc.update("UPDATE part_imports SET status = 'CANCELLED', updated_ts = CURRENT_TIMESTAMP WHERE id = ?", id);
            store.discardRows(id);
        });
        return store.get(id);
    }

    public ResponseEntity<CsvUploadResult> uploadLegacy(MultipartFile file, String username) {
        UUID id = receive(PartSource.LEGACY_ID, file, username);
        process(id);
        PartImportView parsed = store.get(id);
        if ("READY".equals(parsed.status())) {
            transaction.executeWithoutResult(tx -> {
                Map<String, Object> source = lockSource(PartSource.LEGACY_ID);
                PartImportView job = store.lock(id);
                requireReady(job, source, true);
                jdbc.update("UPDATE part_imports SET status = 'APPLYING', accept_skipped_rows = true WHERE id = ?", id);
            });
            publish(id, false);
        }
        PartImportView job = store.get(id);
        CsvUploadResult result = new CsvUploadResult();
        result.setImportId(id);
        result.setSourceId(PartSource.LEGACY_ID);
        result.setSkippedDetailsTruncated(job.skipped() > 1000);
        boolean completed = "COMPLETED".equals(job.status());
        result.setSaved(completed ? Math.toIntExact(job.added()) : 0);
        result.setUpdated(completed ? Math.toIntExact(job.updated() + job.duplicates()) : 0);
        result.setSkipped(Math.toIntExact(job.skipped()));
        result.setSkippedDetails(store.errors(id, org.springframework.data.domain.PageRequest.of(0, 1000)).getContent());
        if (!completed) {
            result.setSkippedDetails(new ArrayList<>(result.getSkippedDetails()));
            result.getSkippedDetails().add(new by.baykulbackend.database.dto.product.SkippedRow(0,
                    job.errorMessage() == null ? "Import could not be published" : job.errorMessage(), ""));
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.status(job.skipped() > 0 ? HttpStatus.MULTI_STATUS : HttpStatus.OK).body(result);
    }

    public void fail(UUID id, String message) {
        transaction.executeWithoutResult(tx -> {
            PartImportView job = store.lock(id);
            if (!List.of("COMPLETED", "CANCELLED", "FAILED").contains(job.status())) {
                jdbc.update("UPDATE part_imports SET status = 'FAILED', error_message = ?, updated_ts = CURRENT_TIMESTAMP WHERE id = ?",
                        truncate(message, 1000), id);
                store.discardRows(id);
            }
        });
    }

    public void recoverInterrupted() {
        List<UUID> interrupted = jdbc.queryForList("SELECT id FROM part_imports WHERE status IN ('QUEUED','PROCESSING','APPLYING')", UUID.class);
        for (UUID id : interrupted) {
            fail(id, "Server restarted during import; upload the file again. The current catalog is unchanged");
            deleteFile(id);
        }
    }

    public void cleanup() {
        List<UUID> expired = jdbc.queryForList("SELECT id FROM part_imports WHERE status = 'READY' AND created_ts < CURRENT_TIMESTAMP - INTERVAL '24 hours'", UUID.class);
        for (UUID id : expired) {
            transaction.executeWithoutResult(tx -> {
                if ("READY".equals(store.lock(id).status())) {
                    jdbc.update("UPDATE part_imports SET status = 'CANCELLED', error_message = 'Preview expired after 24 hours', "
                            + "updated_ts = CURRENT_TIMESTAMP WHERE id = ?", id);
                    store.discardRows(id);
                }
            });
        }
        jdbc.update("DELETE FROM part_import_errors WHERE import_id IN (SELECT id FROM part_imports "
                + "WHERE status IN ('COMPLETED','FAILED','CANCELLED') AND updated_ts < CURRENT_TIMESTAMP - INTERVAL '30 days')");
        if (Files.isDirectory(directory)) {
            try (var files = Files.newDirectoryStream(directory, "*.csv")) {
                for (Path file : files) {
                    String filename = file.getFileName().toString();
                    if (!filename.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.csv")
                            || Files.getLastModifiedTime(file).toInstant().isAfter(java.time.Instant.now().minusSeconds(86400))) {
                        continue;
                    }
                    UUID id = UUID.fromString(filename.substring(0, 36));
                    boolean processing = Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM part_imports "
                            + "WHERE id = ? AND status IN ('QUEUED','PROCESSING','APPLYING'))", Boolean.class, id));
                    if (!processing) {
                        deleteFile(id);
                    }
                }
            } catch (IOException ex) {
                log.warn("Could not clean expired import files", ex);
            }
        }
    }

    private Path filePath(UUID id) {
        return directory.resolve(id + ".csv");
    }

    private void deleteFile(UUID id) {
        try {
            Files.deleteIfExists(filePath(id));
        } catch (IOException ignored) {
            // A cancelled Windows upload may still be open; processing's finally block retries deletion.
        }
    }

    private static String truncate(String text, int limit) {
        return text == null ? "Import failed" : text.substring(0, Math.min(text.length(), limit));
    }
}
