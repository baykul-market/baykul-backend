package by.baykulbackend.database.repository.order;

import by.baykulbackend.database.dao.order.BoxStatus;
import by.baykulbackend.database.dao.order.OrderProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface IOrderProductRepository extends JpaRepository<OrderProduct, UUID> {
    boolean existsByNumber(Long number);
    Optional<OrderProduct> findByBillIsNullAndIdAndStatusIn(UUID id, Collection<BoxStatus> statuses);

    // For searching
    @Query("SELECT op FROM OrderProduct op WHERE CAST(op.number AS string) LIKE :number%")
    Page<OrderProduct> findAllByNumberStartingWith(@Param("number") String number, Pageable pageable);

    Page<OrderProduct> findAllByStatus(BoxStatus status, Pageable pageable);

    Page<OrderProduct> findAllByBillIsNullAndStatusIn(Collection<BoxStatus> statuses, Pageable pageable);

    @Query("SELECT op FROM OrderProduct op WHERE CAST(op.number AS string) LIKE :number% AND op.status = :status")
    Page<OrderProduct> findAllByNumberStartingWithAndStatus(@Param("number") String number, @Param("status") BoxStatus status, Pageable pageable);

    Page<OrderProduct> findAllByStatusAndBillIsNull(BoxStatus status, Pageable pageable);

    @Query("SELECT op FROM OrderProduct op WHERE op.bill IS NULL AND op.status IN :statuses AND CAST(op.number AS string) LIKE :number%")
    Page<OrderProduct> findAllByBillIsNullAndStatusInAndNumberStartingWith(@Param("statuses") Collection<BoxStatus> statuses, @Param("number") String number, Pageable pageable);

    @Query("SELECT op FROM OrderProduct op WHERE op.bill IS NULL AND op.status = :status AND CAST(op.number AS string) LIKE :number%")
    Page<OrderProduct> findAllByBillIsNullAndStatusAndNumberStartingWith(@Param("status") BoxStatus status, @Param("number") String number, Pageable pageable);
}
