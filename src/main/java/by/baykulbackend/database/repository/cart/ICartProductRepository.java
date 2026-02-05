package by.baykulbackend.database.repository.cart;

import by.baykulbackend.database.dao.cart.Cart;
import by.baykulbackend.database.dao.cart.CartProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ICartProductRepository extends JpaRepository<CartProduct, UUID> {
    void deleteAllByCart(Cart cart);
}
