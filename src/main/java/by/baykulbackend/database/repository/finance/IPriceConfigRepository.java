package by.baykulbackend.database.repository.finance;

import by.baykulbackend.database.dao.finance.PriceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IPriceConfigRepository extends JpaRepository<PriceConfig, UUID> {
    @Query("SELECT pc FROM PriceConfig pc ORDER BY pc.createdTs DESC LIMIT 1")
    Optional<PriceConfig> findFirst();
}