package by.baykulbackend.database.repository.finance;

import by.baykulbackend.database.dao.finance.DeliveryCostConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IDeliveryCostConfigRepository extends JpaRepository<DeliveryCostConfig, UUID> {

    /**
     * Finds the best matching global delivery cost rule for the given price.
     * Selects the rule with the highest minimumSum that is still <= the provided price
     * and where user_id IS NULL (i.e. a global/default rule).
     *
     * @param price the order amount in delivery currency
     * @return the best matching global rule, or empty if none applies
     */
    @Query("SELECT dcc FROM DeliveryCostConfig dcc " +
            "WHERE dcc.minimumSum <= :price " +
            "AND dcc.user IS NULL " +
            "ORDER BY dcc.minimumSum DESC LIMIT 1")
    Optional<DeliveryCostConfig> findDeliveryCost(@Param("price") BigDecimal price);

    /**
     * Implements the Fallback Model for per-user delivery rates.
     *
     * <p>Strategy (two-phase lookup):
     * <ol>
     *   <li>First, try to find the best rule among <em>personal</em> rules for {@code userId}.</li>
     *   <li>If no personal rule applies, fall back to the best <em>global</em> rule (user IS NULL).</li>
     * </ol>
     *
     * <p>The JPQL orders results so that personal rules ({@code user.id = :userId}) appear
     * before global rules ({@code user IS NULL}), and among each group the highest
     * {@code minimumSum} wins. We then take the very first row ({@code LIMIT 1}).
     *
     * @param price  the order amount in delivery currency
     * @param userId the user for whom to look up a delivery rate
     * @return the best matching rule (personal first, then global fallback), or empty if none
     */
    @Query("SELECT dcc FROM DeliveryCostConfig dcc " +
            "WHERE dcc.minimumSum <= :price " +
            "AND (dcc.user.id = :userId OR dcc.user IS NULL) " +
            "ORDER BY " +
            "  CASE WHEN dcc.user.id = :userId THEN 0 ELSE 1 END ASC, " +
            "  dcc.minimumSum DESC " +
            "LIMIT 1")
    Optional<DeliveryCostConfig> findDeliveryCost(@Param("price") BigDecimal price, @Param("userId") UUID userId);

    /**
     * Returns all delivery cost rules for a specific user, ordered ascending by minimumSum.
     *
     * @param userId the target user's UUID
     * @return list of personal delivery rules for the user
     */
    List<DeliveryCostConfig> findAllByUserIdOrderByMinimumSumAsc(UUID userId);

    /**
     * Returns all global (non-user-specific) delivery cost rules, ordered ascending by minimumSum.
     *
     * @return list of global delivery rules
     */
    List<DeliveryCostConfig> findAllByUserIsNullOrderByMinimumSumAsc();

    /**
     * Returns all delivery cost rules (both global and user-specific), ordered ascending by minimumSum.
     *
     * @return all delivery rules
     */
    List<DeliveryCostConfig> findAllByOrderByMinimumSumAsc();

    /**
     * Checks whether a delivery cost rule already exists for the given minimumSum,
     * optionally excluding a set of IDs (used to prevent duplicates on update).
     *
     * @param minimumSum the minimum order sum threshold to check
     * @param ids        IDs to exclude from the duplicate check
     * @return true if a conflicting rule exists
     */
    boolean existsByMinimumSumAndUserIsNullAndIdNotIn(BigDecimal minimumSum, Collection<UUID> ids);

    /**
     * Checks whether a user-specific delivery cost rule already exists for the given minimumSum and user.
     *
     * @param minimumSum the minimum order sum threshold to check
     * @param userId     the user UUID
     * @param ids        IDs to exclude from the duplicate check
     * @return true if a conflicting rule exists for that user
     */
    boolean existsByMinimumSumAndUserIdAndIdNotIn(BigDecimal minimumSum, UUID userId, Collection<UUID> ids);
}