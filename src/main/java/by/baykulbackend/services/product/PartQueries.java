package by.baykulbackend.services.product;

import by.baykulbackend.database.dao.product.Part;
import by.baykulbackend.database.dao.product.PartSource;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;
import java.util.UUID;

public final class PartQueries {
    private PartQueries() { }

    public static Specification<Part> available() {
        return (root, query, cb) -> cb.and(cb.isTrue(root.get("catalogPresent")),
                cb.equal(root.get("source").get("status"), PartSource.Status.ACTIVE));
    }

    public static Specification<Part> source(UUID sourceId) {
        return (root, query, cb) -> cb.equal(root.get("source").get("id"), sourceId);
    }

    public static Specification<Part> exact(String field, String value) {
        return (root, query, cb) -> cb.equal(root.get(field), value);
    }

    public static Specification<Part> contains(String field, String value) {
        return (root, query, cb) -> {
            if (value == null || value.isBlank()) {
                return cb.conjunction();
            }
            String escaped = value.toLowerCase(Locale.ROOT).replace("\\", "\\\\")
                    .replace("%", "\\%").replace("_", "\\_");
            return cb.like(cb.lower(root.get(field)), "%" + escaped + "%", '\\');
        };
    }

    public static Specification<Part> text(String text) {
        return contains("article", text).or(contains("name", text)).or(contains("brand", text));
    }
}
