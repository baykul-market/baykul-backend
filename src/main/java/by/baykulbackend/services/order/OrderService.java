package by.baykulbackend.services.order;

import by.baykulbackend.database.dao.cart.Cart;
import by.baykulbackend.database.dao.cart.CartProduct;
import by.baykulbackend.database.dao.order.BoxStatus;
import by.baykulbackend.database.dao.order.Order;
import by.baykulbackend.database.dao.order.OrderProduct;
import by.baykulbackend.database.dao.order.OrderStatus;
import by.baykulbackend.database.dao.product.Part;
import by.baykulbackend.database.dao.user.User;
import by.baykulbackend.database.repository.cart.ICartProductRepository;
import by.baykulbackend.database.repository.cart.ICartRepository;
import by.baykulbackend.database.repository.order.IOrderProductRepository;
import by.baykulbackend.database.repository.order.IOrderRepository;
import by.baykulbackend.database.repository.product.IPartRepository;
import by.baykulbackend.database.repository.user.IUserRepository;
import by.baykulbackend.exceptions.BadRequestException;
import by.baykulbackend.exceptions.NotFoundException;
import by.baykulbackend.services.user.AuthService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final IOrderRepository iOrderRepository;
    private final IOrderProductRepository iOrderProductRepository;
    private final ICartRepository iCartRepository;
    private final ICartProductRepository iCartProductRepository;
    private final IUserRepository iUserRepository;
    private final IPartRepository iPartRepository;
    private final AuthService authService;

    private static final Long START_ORDER_NUMBER = 100000L;

    /**
     * Creates a new order from the user's cart
     * @return ResponseEntity with success/error message
     * @throws NotFoundException if user or cart not found
     * @throws BadRequestException if user's cart is empty
     */
    @Transactional
    public ResponseEntity<?> createOrderFromCart() {
        Map<String, Object> response = new HashMap<>();

        User user = iUserRepository.findByLogin(authService.getAuthInfo().getPrincipal().toString())
                .orElseThrow(() -> new NotFoundException("User not found"));

        Cart cart = iCartRepository.findByUserLoginWithLock(user.getLogin())
                .orElseThrow(() -> new NotFoundException("Cart not found"));

        if (cart.getCartProducts() == null || cart.getCartProducts().isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        List<OrderProduct> orderProducts = new ArrayList<>();
        List<Map<String, Object>> unavailableProducts = new ArrayList<>();
        boolean hasAvailableProducts = false;

        for (CartProduct cartProduct : cart.getCartProducts()) {
            Part partFromDb = iPartRepository.findById(cartProduct.getPart().getId())
                    .orElseGet(() -> {
                        Part emptyPart = new Part();
                        emptyPart.setStorageCount(0);
                        return emptyPart;
                    });
            Integer requestedCount = cartProduct.getPartsCount();
            Integer availableCount = partFromDb.getStorageCount();

            if (availableCount != null && availableCount < requestedCount) {
                Map<String, Object> unavailableProduct = new HashMap<>();
                unavailableProduct.put("part_id", cartProduct.getPart().getId().toString());
                unavailableProducts.add(unavailableProduct);
            } else {
                hasAvailableProducts = true;
                
                OrderProduct orderProduct = new OrderProduct();
                orderProduct.setPart(partFromDb);
                orderProduct.setPartsCount(requestedCount);
                orderProduct.setStatus(BoxStatus.ORDERED);
                orderProduct.setPrice(partFromDb.getPrice().multiply(BigDecimal.valueOf(requestedCount)));
                orderProduct.setCurrency(partFromDb.getCurrency());
                orderProducts.add(orderProduct);
            }
        }

        if (!hasAvailableProducts) {
            response.put("create_order", "false");
            response.put("error", "No products available in storage");
            response.put("unavailable_products", unavailableProducts);
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }

        Long orderNumber = generateOrderNumber();

        Order order = new Order();
        order.setUser(user);
        order.setNumber(orderNumber);
        order.setStatus(OrderStatus.CREATED);
        order = iOrderRepository.save(order);

        for (OrderProduct orderProduct : orderProducts) {
            orderProduct.setOrder(order);
            Part part = orderProduct.getPart();

            if (part.getStorageCount() != null) {
                Integer newStorageCount = part.getStorageCount() - orderProduct.getPartsCount();
                part.setStorageCount(newStorageCount);
                iPartRepository.save(part);
            }

            iOrderProductRepository.save(orderProduct);
        }

        iCartProductRepository.deleteAllByCartId(cart.getId());

        response.put("create_order", "true");
        response.put("id", order.getId().toString());

        if (!unavailableProducts.isEmpty()) {
            response.put("warning", "Some products were unavailable");
            response.put("unavailable_products", unavailableProducts);
        }

        log.info("Order {} created for user {} -> {}", order.getNumber(), user.getLogin(),
                authService.getAuthInfo().getPrincipal());

        return ResponseEntity.ok(response);
    }

    /**
     * Updates order status (admin only)
     * @param id order ID
     * @param order the Order object containing updated fields
     * @return ResponseEntity with success/error message
     * @throws NotFoundException if order not found
     * @throws BadRequestException if data to update is invalid
     */
    @Transactional
    public ResponseEntity<?> updateOrder(UUID id, Order order) {
        Map<String, String> response = new HashMap<>();
        
        Order orderFromDb = iOrderRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (order.getStatus() != null) {
            if (Set.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED, OrderStatus.PAID).contains(order.getStatus())) {
                orderFromDb.setStatus(order.getStatus());
                log.info("Order's status with id {} has been updated -> {}",
                        id, authService.getAuthInfo().getPrincipal());
            } else {
                throw new BadRequestException("Order's status can be changed only to PAID, PROCESSING or CANCELLED");
            }
        }

        iOrderRepository.save(orderFromDb);
        response.put("update_order", "true");

        if (order.getStatus() != null) {
            updateOrderProductsAfterOrderUpdate(orderFromDb);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Updates order product
     * @param id order product ID
     * @param orderProduct the Order product object containing updated fields
     * @return ResponseEntity with success/error message
     * @throws BadRequestException if found validation errors
     */
    @Transactional
    public ResponseEntity<?> updateOrderProduct(UUID id, OrderProduct orderProduct) {
        Map<String, String> response = new HashMap<>();
        
        OrderProduct orderProductFromDb = iOrderProductRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order product not found"));

        if (orderProduct.getNumber() != null) {
            if (orderProduct.getNumber() < START_ORDER_NUMBER) {
                throw new BadRequestException("Order product number must be greater or equal 100 000");
            }

            if (!orderProduct.getNumber().equals(orderProductFromDb.getNumber())
                    && iOrderProductRepository.existsByNumber(orderProduct.getNumber())) {
                throw new BadRequestException("Order product with that number already exists");
            }
        }

        if (orderProduct.getStatus() != null) {
            if (!orderProductFromDb.getStatus().getNextStatuses().contains(orderProduct.getStatus())) {
                throw new BadRequestException("Order product's status transition not allowed");
            }

            orderProductFromDb.setStatus(orderProduct.getStatus());
            log.info("Order product's status with id {} has been updated -> {}",
                    id, authService.getAuthInfo().getPrincipal());
        }

        if (orderProduct.getNumber() != null && !orderProduct.getNumber().equals(orderProductFromDb.getNumber())) {
            orderProductFromDb.setNumber(orderProduct.getNumber());
            log.info("Order product's number with id {} has been updated -> {}",
                    id, authService.getAuthInfo().getPrincipal());
        }

        if (orderProduct.getPartsCount() != null
                && !orderProduct.getPartsCount().equals(orderProductFromDb.getPartsCount())) {
            orderProductFromDb.setPartsCount(orderProduct.getPartsCount());
            log.info("Order product's part's count with id {} has been updated -> {}",
                    id, authService.getAuthInfo().getPrincipal());
        }

        iOrderProductRepository.save(orderProductFromDb);
        response.put("update_order_product", "true");

        if (orderProduct.getStatus() != null) {
            updateOrderAfterOrderProductUpdate(orderProductFromDb.getOrder());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Update order status if all order products were delivered
     * @param order order object to update
     */
    private void updateOrderAfterOrderProductUpdate(Order order) {
        List<OrderProduct> orderProducts = order.getOrderProducts();

        boolean allDelivered = orderProducts.stream()
                .allMatch(op ->
                        Set.of(BoxStatus.DELIVERED, BoxStatus.CANCELLED, BoxStatus.RETURNED).contains(op.getStatus())
                );

        if (allDelivered && order.getStatus() != OrderStatus.COMPLETED) {
            order.setStatus(OrderStatus.COMPLETED);
            iOrderRepository.save(order);
            log.info("Order {} automatically completed as all products are delivered", order.getNumber());
        }
    }

    /**
     * Update order products status if order was cancelled
     * @param order order object to update
     */
    private void updateOrderProductsAfterOrderUpdate(Order order) {
        if (order.getStatus() == OrderStatus.CANCELLED) {
            List<OrderProduct> orderProducts = order.getOrderProducts();

            for (OrderProduct orderProduct : orderProducts) {
                BoxStatus newStatus = determineCancellationStatus(orderProduct);

                if (orderProduct.getStatus() != newStatus) {
                    orderProduct.setStatus(newStatus);
                    iOrderProductRepository.save(orderProduct);
                    log.info("Order product {} status automatically updated to {} due to order cancellation",
                            orderProduct.getId(), newStatus);
                }
            }
        }
    }

    /**
     * Determines cancellation status for orderProduct in case of order cancellation
     * @param orderProduct orderProduct object to check
     * @return needed status
     */
    private BoxStatus determineCancellationStatus(OrderProduct orderProduct) {
        BoxStatus currentStatus = orderProduct.getStatus();

        if (currentStatus == BoxStatus.CANCELLED || currentStatus == BoxStatus.RETURNED) {
            return currentStatus;
        }

        if (currentStatus.getNextStatuses().contains(BoxStatus.CANCELLED)) {
            return BoxStatus.CANCELLED;
        }

        if (currentStatus.getNextStatuses().contains(BoxStatus.RETURNED)) {
            return BoxStatus.RETURNED;
        }

        return currentStatus;
    }

    /**
     * Generates unique order number
     * @return generated order number
     */
    private Long generateOrderNumber() {
        Long maxNumber = iOrderRepository.findAll().stream()
                .map(Order::getNumber)
                .filter(Objects::nonNull)
                .max(Long::compare)
                .orElse(START_ORDER_NUMBER - 1);
        
        return maxNumber + 1;
    }
}