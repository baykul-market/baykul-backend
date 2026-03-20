package by.baykulbackend.services.cart;

import by.baykulbackend.database.dao.cart.Cart;
import by.baykulbackend.database.dao.cart.CartProduct;
import by.baykulbackend.database.dao.product.Part;
import by.baykulbackend.database.dao.user.User;
import by.baykulbackend.database.model.Role;
import by.baykulbackend.database.repository.cart.ICartProductRepository;
import by.baykulbackend.database.repository.cart.ICartRepository;
import by.baykulbackend.database.repository.product.IPartRepository;
import by.baykulbackend.database.repository.user.IUserRepository;
import by.baykulbackend.exceptions.NotFoundException;
import by.baykulbackend.services.finance.PriceService;
import by.baykulbackend.services.user.AuthService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {
    private final ICartRepository iCartRepository;
    private final ICartProductRepository iCartProductRepository;
    private final IUserRepository iUserRepository;
    private final IPartRepository iPartRepository;
    private final AuthService authService;
    private final PriceService priceService;

    public Cart getMy() {
        User user = iUserRepository.findByLogin(authService.getAuthInfo().getPrincipal().toString())
                .orElseThrow(() -> new NotFoundException("User not found"));

        Cart cart = iCartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new NotFoundException("Cart not found"));

        for (CartProduct cartProduct : cart.getCartProducts()) {
            Part part = cartProduct.getPart();

            boolean needsDelivery = part.getStorageCount() == null
                    || cartProduct.getPartsCount() > part.getStorageCount();

            part.setPrice(priceService.calculateProductPrice(part, needsDelivery));
            part.setCurrency(priceService.getSystemCurrency());
        }

        return cart;
    }

    /**
     * Adds a part to a specific cart.
     *
     * @param cartId the UUID of the cart to add the part to
     * @param partId the UUID of the part to add
     * @return ResponseEntity with success/error message
     * @throws NotFoundException if cart or part is not found
     */
    @Transactional
    public ResponseEntity<?> addPartToCart(UUID cartId, UUID partId) {
        Cart cart = iCartRepository.findByIdWithLock(cartId)
                .orElseThrow(() -> new NotFoundException("Cart not found"));
        Part part = iPartRepository.findById(partId)
                .orElseThrow(() -> new NotFoundException("Part not found"));

        return addPartToCart(cart, part);
    }

    /**
     * Adds a part to the currently authenticated user's cart.
     *
     * @param partId the UUID of the part to add
     * @return ResponseEntity with success/error message
     * @throws NotFoundException if user's cart or part is not found
     */
    @Transactional
    public ResponseEntity<?> addPartToCart(UUID partId) {
        Cart cart = iCartRepository.findByUserLoginWithLock(authService.getAuthInfo().getPrincipal().toString())
                .orElseThrow(() -> new NotFoundException("User's cart not found"));
        Part partFromDB = iPartRepository.findById(partId)
                .orElseThrow(() -> new NotFoundException("Part not found"));

        return addPartToCart(cart, partFromDB);
    }

    /**
     * Updates a cart product in the currently authenticated user's cart.
     * Validates that the cart product belongs to the user's cart.
     *
     * @param id the UUID of the cart product to update
     * @param cartProduct the CartProduct object containing updated fields
     * @return ResponseEntity with success/error message
     * @throws NotFoundException if cart or cart product is not found
     */
    @Transactional
    public ResponseEntity<?> updateUsersCartProductById(UUID id, CartProduct cartProduct) {
        Map<String, String> response = new HashMap<>();

        if (isNotValidCartProduct(cartProduct, response)) {
            response.put("update_cart", "false");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        Cart cart = iCartRepository.findByUserLogin(authService.getAuthInfo().getPrincipal().toString())
                .orElseThrow(() -> new NotFoundException("User's cart not found"));

        CartProduct cartProductFromDB = cart.getCartProducts()
                .stream()
                .filter(cp -> cp.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Cart product does not exist in user's cart"));

        if (cartProduct.getPartsCount() != null) {
            cartProductFromDB.setPartsCount(cartProduct.getPartsCount());
            log.info("Cart product's count of parts with id {} has been updated -> {}", cartProduct.getId(),
                    authService.getAuthInfo().getPrincipal());
        }

        iCartProductRepository.save(cartProductFromDB);
        response.put("update_cart", "true");

        return ResponseEntity.ok(response);
    }

    /**
     * Updates a cart product by ID (admin function).
     * Does not validate user ownership.
     *
     * @param id the UUID of the cart product to update
     * @param cartProduct the CartProduct object containing updated fields
     * @return ResponseEntity with success/error message
     * @throws NotFoundException if cart product is not found
     */
    @Transactional
    public ResponseEntity<?> updateCartProductById(UUID id, CartProduct cartProduct) {
        Map<String, String> response = new HashMap<>();

        if (isNotValidCartProduct(cartProduct, response)) {
            response.put("update_cart", "false");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        CartProduct cartProductFromDB = iCartProductRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cart product not found"));

        if (cartProduct.getPartsCount() != null) {
            cartProductFromDB.setPartsCount(cartProduct.getPartsCount());
            log.info("Cart product's count of parts with id {} has been updated -> {}", cartProduct.getId(),
                    authService.getAuthInfo().getPrincipal());
        }

        iCartProductRepository.save(cartProductFromDB);
        response.put("update_cart", "true");

        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a cart product from the currently authenticated user's cart.
     * Validates that the cart product belongs to the user's cart.
     *
     * @param id the UUID of the cart product to delete
     * @return ResponseEntity with success/error message
     * @throws NotFoundException if cart is not found
     */
    @Transactional
    public ResponseEntity<?> deleteUsersCartProductById(UUID id) {
        Map<String, String> response = new HashMap<>();

        Cart cart = iCartRepository.findByUserLogin(authService.getAuthInfo().getPrincipal().toString())
                .orElseThrow(() -> new NotFoundException("User's cart not found"));

        if (cart.getCartProducts().stream().noneMatch(cp -> cp.getId().equals(id))) {
            response.put("delete_cart_product", "false");
            response.put("error", "Cart product does not exist in user's cart");
        }

        iCartProductRepository.deleteById(id);
        response.put("delete_cart_product", "true");
        log.info("Delete cart product with id = {} -> {}", id, authService.getAuthInfo().getPrincipal());

        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a cart product by ID (admin function).
     * Does not validate user ownership.
     *
     * @param id the UUID of the cart product to delete
     * @return ResponseEntity with success/error message
     * @throws NotFoundException if cart product is not found
     */
    @Transactional
    public ResponseEntity<?> deleteCartProductById(UUID id) {
        Map<String, String> response = new HashMap<>();

        CartProduct cartProductFromDB = iCartProductRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cart product not found"));

        iCartProductRepository.deleteById(id);
        response.put("delete_cart_product", "true");
        log.info("Delete cart product with id = {} -> {}", id, authService.getAuthInfo().getPrincipal());

        return ResponseEntity.ok(response);
    }

    /**
     * Clears all cart products from the currently authenticated user's cart.
     *
     * @return ResponseEntity with success/error message
     * @throws NotFoundException if user's cart is not found
     */
    @Transactional
    public ResponseEntity<?> clearUsersCart() {
        Map<String, String> response = new HashMap<>();

        Cart cart = iCartRepository.findByUserLogin(authService.getAuthInfo().getPrincipal().toString())
                .orElseThrow(() -> new NotFoundException("User's cart not found"));

        iCartProductRepository.deleteAllByCartId(cart.getId());
        response.put("clear_cart", "true");
        log.info("Clear cart product with id = {} -> {}", cart.getId(), authService.getAuthInfo().getPrincipal());

        return ResponseEntity.ok(response);
    }

    /**
     * Adds a part to a cart.
     * If the part already exists in the cart, increases its quantity.
     * Checks if part is available in storage.
     *
     * @param cart the Cart to add the part to
     * @param part the Part to add
     * @return ResponseEntity with success/error message
     */
    private ResponseEntity<?> addPartToCart(Cart cart, Part part) {
        Map<String, String> response = new HashMap<>();

        if (part.getStorageCount() != null && part.getStorageCount() == 0) {
            response.put("add_cart", "false");
            response.put("storage_empty", "true");
            response.put("error", "Part storage count is zero");
            log.warn("Part's storage count with id {} is zero -> {}", part.getId(),
                    authService.getAuthInfo().getPrincipal());
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }

        CartProduct cartProduct = cart.getCartProducts()
                .stream()
                .filter(cp -> cp.getPart().equals(part))
                .findFirst()
                .orElse(null);

        if (cartProduct != null) {
            cartProduct.setPartsCount(cartProduct.getPartsCount() + 1);

            response.put("add_cart", "false");
            response.put("increase_count", "Cart product already exists. Count increased to " +
                    cartProduct.getPartsCount());
            log.warn("Cart product in cart with id {} with part id {} already exists -> {}", cart.getId(), part.getId(),
                    authService.getAuthInfo().getPrincipal());
        } else {
            cartProduct = new CartProduct();
            cartProduct.setPart(part);
            cartProduct.setCart(cart);
            cartProduct.setPartsCount(1);

            response.put("add_cart", "true");
            log.info("Added to cart with id {} part with id {} -> {}", cart.getId(), part.getId(),
                    authService.getAuthInfo().getPrincipal());
        }

        iCartProductRepository.save(cartProduct);

        return ResponseEntity.ok(response);
    }

    /**
     * Validates if a user is eligible for a new cart.
     * Checks user role and existing cart.
     *
     * @param user the User to validate
     * @param response Map to collect validation error messages
     * @return true if user is not valid for cart creation, false otherwise
     */
    private boolean isNotValidNewCart(User user, Map<String, String> response) {
        if (!user.getRole().equals(Role.USER)) {
            response.put("create_cart", "false");
            response.put("warn", "User with id is illegal to have a cart");
            log.warn("User with id {} is illegal to have a cart -> {}", user.getId(),
                    authService.getAuthInfo().getPrincipal());
            return true;
        }

        if (user.getCart() != null) {
            response.put("create_cart", "false");
            response.put("warn", "Cart already exists");
            log.warn("Cart for user with id {} already exists -> {}", user.getId(),
                    authService.getAuthInfo().getPrincipal());
            return true;
        }

        return false;
    }

    /**
     * Validates cart product data.
     * Checks that parts count is positive if provided.
     *
     * @param cartProduct the CartProduct to validate
     * @param response Map to collect validation error messages
     * @return true if cart product is not valid, false otherwise
     */
    private boolean isNotValidCartProduct(CartProduct cartProduct, Map<String, String> response) {
        if (cartProduct.getPartsCount() != null && cartProduct.getPartsCount() <= 0) {
            response.put("error", "Parts count must be greater than zero");
            return true;
        }

        return false;
    }
}