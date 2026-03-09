package by.baykulbackend.database.repository.cart;

import by.baykulbackend.database.dao.cart.Cart;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ICartRepository extends JpaRepository<Cart, UUID> {
    Optional<Cart> findByUserId(UUID userId);
    Optional<Cart> findByUserLogin(String userLogin);
    boolean existsByUserId(UUID userId);
    boolean existsByUserLogin(String userLogin);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cart c WHERE c.user.login = :userLogin")
    Optional<Cart> findByUserLoginWithLock(@Param("userLogin") String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cart c WHERE c.id = :id")
    Optional<Cart> findByIdWithLock(@Param("id") UUID id);
}
