package by.baykulbackend.services.product;

import by.baykulbackend.database.dao.product.Part;
import by.baykulbackend.database.dao.product.PartSource;
import by.baykulbackend.exceptions.CatalogConflictException;
import by.baykulbackend.exceptions.NotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class PartCatalogGuard {
    private final EntityManager entityManager;

    public PartSource lockSource(UUID id, boolean write) {
        PartSource source = entityManager.find(PartSource.class, id);
        if (source == null) {
            throw new NotFoundException("Source not found");
        }
        entityManager.refresh(source, write ? LockModeType.PESSIMISTIC_WRITE : LockModeType.PESSIMISTIC_READ);
        return source;
    }

    public PartSource writableSource(UUID id) {
        PartSource source = lockSource(id, true);
        if (source.getStatus() == PartSource.Status.ARCHIVED) {
            throw new CatalogConflictException("SOURCE_ARCHIVED", "Source is archived");
        }
        // Force a version change even when two edits happen in the same clock tick.
        source.setUpdatedTs(LocalDateTime.now());
        entityManager.lock(source, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        return source;
    }

    public void requireAvailable(Collection<Part> parts) {
        parts.stream().map(p -> p.getSource().getId()).distinct().sorted()
                .forEach(id -> lockSource(id, false));
        for (Part part : parts) {
            entityManager.refresh(part);
            if (!part.isAvailable()) {
                throw new CatalogConflictException("PART_UNAVAILABLE", "Part is no longer available: " + part.getArticle());
            }
        }
    }

    public void refreshForEdit(Part part) {
        writableSource(part.getSource().getId());
        // Publication may have committed while the source lock was being acquired.
        entityManager.refresh(part);
    }
}
