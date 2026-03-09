package by.baykulbackend.database.repository.order;

import by.baykulbackend.database.dao.order.BoxStatus;
import by.baykulbackend.database.dao.order.OrderProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IOrderProductRepository extends JpaRepository<OrderProduct, UUID> {
    Optional<OrderProduct> findByNumber(Long number);
    Page<OrderProduct> findAllByStatus(BoxStatus status, Pageable pageable);
    boolean existsByNumber(Long number);
    Page<OrderProduct> findAllByBillIsNullAndStatusIn(List<BoxStatus> statuses, Pageable pageable);
    Optional<OrderProduct> findByBillIsNullAndIdAndStatusIn(UUID id, List<BoxStatus> statuses);
}
