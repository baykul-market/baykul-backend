package by.baykulbackend.services.product;

import by.baykulbackend.database.dto.product.PartImportView;
import by.baykulbackend.database.dto.product.SkippedRow;
import by.baykulbackend.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PartImportStore {
    private final JdbcTemplate jdbc;

    public static final RowMapper<PartImportView> MAPPER = (rs, index) -> new PartImportView(
            rs.getObject("id", UUID.class), rs.getObject("source_id", UUID.class), rs.getString("filename"),
            rs.getString("uploaded_by"), rs.getString("status"), rs.getLong("source_version"),
            rs.getLong("total_rows"), rs.getLong("valid_rows"), rs.getLong("skipped"), rs.getLong("duplicates"),
            rs.getLong("added"), rs.getLong("updated"), rs.getLong("removed"), rs.getString("error_message"),
            rs.getTimestamp("created_ts").toLocalDateTime(), rs.getTimestamp("updated_ts").toLocalDateTime());

    public PartImportView get(UUID sourceId, UUID id) {
        PartImportView job = get(id);
        if (!job.sourceId().equals(sourceId)) {
            throw new NotFoundException("Import not found");
        }
        return job;
    }

    public PartImportView get(UUID id) {
        return jdbc.query("SELECT * FROM part_imports WHERE id = ?", MAPPER, id).stream().findFirst()
                .orElseThrow(() -> new NotFoundException("Import not found"));
    }

    public PartImportView lock(UUID id) {
        return jdbc.query("SELECT * FROM part_imports WHERE id = ? FOR UPDATE", MAPPER, id).stream().findFirst()
                .orElseThrow(() -> new NotFoundException("Import not found"));
    }

    public Page<PartImportView> history(UUID sourceId, Pageable page) {
        long count = jdbc.queryForObject("SELECT count(*) FROM part_imports WHERE source_id = ?", Long.class, sourceId);
        return new PageImpl<>(jdbc.query("SELECT * FROM part_imports WHERE source_id = ? ORDER BY created_ts DESC, id DESC LIMIT ? OFFSET ?",
                MAPPER, sourceId, page.getPageSize(), page.getOffset()), page, count);
    }

    public PartImportView latest(UUID sourceId) {
        return jdbc.query("SELECT * FROM part_imports WHERE source_id = ? ORDER BY created_ts DESC, id DESC LIMIT 1",
                MAPPER, sourceId).stream().findFirst().orElse(null);
    }

    public Page<SkippedRow> errors(UUID id, Pageable page) {
        long count = jdbc.queryForObject("SELECT count(*) FROM part_import_errors WHERE import_id = ?", Long.class, id);
        List<SkippedRow> rows = jdbc.query("SELECT * FROM part_import_errors WHERE import_id = ? ORDER BY row_number LIMIT ? OFFSET ?",
                (rs, index) -> new SkippedRow(rs.getInt("row_number"), rs.getString("error_message"), rs.getString("raw_data")),
                id, page.getPageSize(), page.getOffset());
        return new PageImpl<>(rows, page, count);
    }

    public void discardRows(UUID id) {
        jdbc.update("DELETE FROM part_import_rows WHERE import_id = ?", id);
    }
}
