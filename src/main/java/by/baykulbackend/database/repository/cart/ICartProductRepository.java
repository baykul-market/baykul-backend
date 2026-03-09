package by.baykulbackend.database.repository.cart;

import by.baykulbackend.database.dao.cart.CartProduct;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ICartProductRepository extends JpaRepository<CartProduct, UUID> {
    @Modifying
    @Transactional
    @Query("DELETE FROM CartProduct cp WHERE cp.cart.id = :cartId")
    void deleteAllByCartId(@Param("cartId") UUID cartId);

    @Modifying
    @Transactional
    @Query("DELETE FROM CartProduct cp WHERE cp.id = :id")
    void deleteById(@Param("id") UUID id);
}
