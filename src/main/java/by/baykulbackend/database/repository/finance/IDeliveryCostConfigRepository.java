package by.baykulbackend.database.repository.finance;

import by.baykulbackend.database.dao.finance.DeliveryCostConfig;
import by.baykulbackend.database.dao.finance.PriceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IDeliveryCostConfigRepository extends JpaRepository<DeliveryCostConfig, UUID> {
    @Query("SELECT dcc FROM DeliveryCostConfig dcc WHERE dcc.minimumSum <= :price ORDER BY dcc.minimumSum DESC LIMIT 1")
    Optional<DeliveryCostConfig> findDeliveryCost(BigDecimal price);

    List<DeliveryCostConfig> findAllByOrderByMinimumSumAsc();
}