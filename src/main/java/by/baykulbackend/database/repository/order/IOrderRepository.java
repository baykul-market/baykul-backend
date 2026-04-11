package by.baykulbackend.database.repository.order;

import by.baykulbackend.database.dao.order.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IOrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByUserId(UUID userId);
    Page<Order> findByUserLogin(String userLogin, Pageable pageable);

    Optional<Order> findByUserLoginAndId(String userLogin, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Order c WHERE c.id = :id")
    Optional<Order> findByIdWithLock(@Param("id") UUID id);
}
