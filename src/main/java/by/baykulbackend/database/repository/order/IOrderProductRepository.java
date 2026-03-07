package by.baykulbackend.database.repository.order;

import by.baykulbackend.database.dao.order.BoxStatus;
import by.baykulbackend.database.dao.order.OrderProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IOrderProductRepository extends JpaRepository<OrderProduct, UUID> {
    Optional<OrderProduct> findByNumber(Long number);
    Page<OrderProduct> findAllByStatus(BoxStatus status, Pageable pageable);
    boolean existsByNumber(Long number);
    Page<OrderProduct> findAllByBillIsNull(Pageable pageable);
    Optional<OrderProduct> findByBillIsNullAndId(UUID id);
}
