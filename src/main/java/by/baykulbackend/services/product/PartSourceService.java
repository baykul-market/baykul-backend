package by.baykulbackend.services.product;

import by.baykulbackend.database.dao.product.PartSource;
import by.baykulbackend.database.dto.product.PartSourceView;
import by.baykulbackend.database.repository.product.IPartSourceRepository;
import by.baykulbackend.exceptions.CatalogConflictException;
import by.baykulbackend.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PartSourceService {
    private final IPartSourceRepository sources;
    private final PartCatalogGuard guard;
    private final PartImportStore imports;
    private final JdbcTemplate jdbc;

    public Page<PartSourceView> list(String text, PartSource.Status status, Pageable page) {
        String search = "%" + (text == null ? "" : text.toLowerCase(java.util.Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")) + "%";
        String filter = " WHERE lower(name) LIKE ? ESCAPE '\\' AND "
                + (status == null ? "status <> 'ARCHIVED'" : "status = ?");
        Object[] params = status == null ? new Object[]{search} : new Object[]{search, status.name()};
        long count = jdbc.queryForObject("SELECT count(*) FROM part_sources" + filter, Long.class, params);
        List<Object> paged = new java.util.ArrayList<>(java.util.Arrays.asList(params));
        paged.add(page.getPageSize());
        paged.add(page.getOffset());
        List<UUID> ids = jdbc.queryForList("SELECT id FROM part_sources" + filter
                + " ORDER BY updated_ts DESC, id DESC LIMIT ? OFFSET ?", UUID.class, paged.toArray());
        return new PageImpl<>(ids.stream().map(this::get).toList(), page, count);
    }

    public PartSourceView get(UUID id) {
        return view(sources.findById(id).orElseThrow(() -> new NotFoundException("Source not found")));
    }

    @Transactional
    public PartSourceView create(String name) {
        PartSource source = new PartSource();
        source.setId(UUID.randomUUID());
        source.setName(name.trim());
        return view(sources.saveAndFlush(source));
    }

    @Transactional
    public PartSourceView update(UUID id, String name, PartSource.Status status) {
        PartSource source = guard.lockSource(id, true);
        if (source.getStatus() == PartSource.Status.ARCHIVED) {
            throw new CatalogConflictException("SOURCE_ARCHIVED", "Archived sources cannot be changed or reactivated");
        }
        if (status == PartSource.Status.ARCHIVED) {
            throw new IllegalArgumentException("Use source deletion to archive a source");
        }
        if (name != null) {
            if (name.isBlank() || name.length() > 255) {
                throw new IllegalArgumentException("Source name must contain 1 to 255 characters");
            }
            source.setName(name.trim());
        }
        if (status != null) {
            source.setStatus(status);
        }
        source.setUpdatedTs(LocalDateTime.now());
        return view(sources.saveAndFlush(source));
    }

    @Transactional
    public PartSourceView archive(UUID id) {
        PartSource source = guard.lockSource(id, true);
        if (source.getStatus() != PartSource.Status.ARCHIVED) {
            source.setStatus(PartSource.Status.ARCHIVED);
            source.setUpdatedTs(LocalDateTime.now());
            sources.saveAndFlush(source);
            List<UUID> pending = jdbc.queryForList("SELECT id FROM part_imports WHERE source_id = ? "
                    + "AND status IN ('QUEUED','PROCESSING','READY','APPLYING') FOR UPDATE", UUID.class, id);
            for (UUID importId : pending) {
                jdbc.update("UPDATE part_imports SET status = 'CANCELLED', error_message = 'Source was archived', "
                        + "updated_ts = CURRENT_TIMESTAMP WHERE id = ?", importId);
                imports.discardRows(importId);
            }
        }
        return view(source);
    }

    private PartSourceView view(PartSource source) {
        long count = jdbc.queryForObject("SELECT count(*) FROM parts WHERE source_id = ? AND catalog_present = true", Long.class, source.getId());
        return new PartSourceView(source.getId(), source.getName(), source.getStatus(), source.isSystemSource(),
                source.getVersion(), count, imports.latest(source.getId()), source.getCreatedTs(), source.getUpdatedTs());
    }
}
