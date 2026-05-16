package by.baykulbackend.database.repository.product;

import by.baykulbackend.database.dao.product.Part;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface IPartRepository extends JpaRepository<Part, UUID> {
    Optional<Part> findByArticle(String article);
    boolean existsByArticle(String article);

    Set<Part> findAllByArticleIn(Set<String> articles);

    /**
     * Fetches parts by articles using a PostgreSQL array parameter.
     * Uses a single bind parameter instead of N parameters in an IN clause,
     * which dramatically reduces parse-tree overhead for large batches.
     *
     * @param articles Array of article strings to look up.
     * @return List of matching Part entities.
     */
    @Query(value = "SELECT * FROM parts WHERE article = ANY(CAST(:articles AS text[]))",
            nativeQuery = true)
    List<Part> findAllByArticleArray(@Param("articles") String[] articles);

    Page<Part> findByName(String name, Pageable pageable);
    Page<Part> findByBrand(String brand, Pageable pageable);
    Page<Part> findByArticleContainingIgnoreCase(String article, Pageable pageable);
    Page<Part> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<Part> findByBrandContainingIgnoreCase(String brand, Pageable pageable);
    Page<Part> findByBrandContainingIgnoreCaseAndNameContainingIgnoreCase(
            String brand, String name, Pageable pageable
    );
    Page<Part> findByArticleContainingIgnoreCaseAndNameContainingIgnoreCase(
            String article, String name, Pageable pageable
    );
    Page<Part> findByBrandContainingIgnoreCaseAndArticleContainingIgnoreCase(
            String brand, String article, Pageable pageable
    );
    Page<Part> findByBrandContainingIgnoreCaseAndNameContainingIgnoreCaseAndArticleContainingIgnoreCase(
            String brand, String name, String article, Pageable pageable
    );
}
