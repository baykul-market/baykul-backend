package by.baykulbackend.services.order;

import by.baykulbackend.database.dao.cart.Cart;
import by.baykulbackend.database.dao.cart.CartProduct;
import by.baykulbackend.database.dao.order.BoxStatus;
import by.baykulbackend.database.dao.order.Order;
import by.baykulbackend.database.dao.order.OrderProduct;
import by.baykulbackend.database.dao.order.OrderStatus;
import by.baykulbackend.database.dao.product.Part;
import by.baykulbackend.database.dao.user.User;
import by.baykulbackend.database.dto.balance.BalanceOperationDto;
import by.baykulbackend.database.dto.order.CreateOrderRequestDto;
import by.baykulbackend.database.repository.cart.ICartProductRepository;
import by.baykulbackend.database.repository.cart.ICartRepository;
import by.baykulbackend.database.repository.order.IOrderProductRepository;
import by.baykulbackend.database.repository.order.IOrderRepository;
import by.baykulbackend.database.repository.product.IPartRepository;
import by.baykulbackend.database.repository.user.IUserRepository;
import by.baykulbackend.exceptions.BadRequestException;
import by.baykulbackend.exceptions.NotFoundException;
import by.baykulbackend.services.user.AuthService;
import by.baykulbackend.database.dao.balance.BalanceOperationType;
import by.baykulbackend.services.balance.BalanceService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final BalanceService balanceService;

    private static final Long START_ORDER_NUMBER = 100000L;

    /**
     * Creates a new order from the user's cart
     * @param request The create order request containing options like payLater
     * @return ResponseEntity with a success/error message
     * @throws NotFoundException if user or cart not found
     * @throws BadRequestException if the user's cart is empty
     */
    @Transactional
    public ResponseEntity<?> createOrderFromCart(CreateOrderRequestDto request) {
        Map<String, Object> response = new HashMap<>();

        User user = getUser();
        Cart cart = getCart(user);

        validateCart(cart);

        var availabilityResult = checkAvailability(cart);
        List<OrderProduct> orderProducts = availabilityResult.orderProducts;
        List<Map<String, Object>> unavailableProducts = availabilityResult.unavailableProducts;

        if (!availabilityResult.hasAvailableProducts) {
            response.put("create_order", "false");
            response.put("error", "No products available in storage");
            response.put("unavailable_products", unavailableProducts);
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }

        BigDecimal totalOrderPrice = calculateTotalOrderPrice(orderProducts);

        Order order = buildAndSaveOrder(user, OrderStatus.CREATED);
        saveOrderProducts(order, orderProducts);

        iCartProductRepository.deleteAllByCartId(cart.getId());

        if (!request.isPayLater() && totalOrderPrice.compareTo(BigDecimal.ZERO) > 0) {
            processPayment(user, order, totalOrderPrice);
            order.setStatus(OrderStatus.PAID);
            iOrderRepository.save(order);
        }

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
     * Pays for an existing order
     * @param orderId The UUID of the order to pay for
     * @return ResponseEntity with success/error message
     */
    @Transactional
    public ResponseEntity<?> payForOrder(UUID orderId) {
        User user = getUser();
        Order order = iOrderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
             throw new BadRequestException("Order does not belong to the current user");
        }

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new BadRequestException("Order is already paid or processed");
        }

        BigDecimal totalOrderPrice = order.getOrderProducts().stream()
                .map(OrderProduct::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        if (totalOrderPrice.compareTo(BigDecimal.ZERO) > 0) {
            processPayment(user, order, totalOrderPrice);
            order.setStatus(OrderStatus.PAID);
            iOrderRepository.save(order);
        }

        Map<String, String> response = new HashMap<>();
        response.put("pay_order", "true");
        return ResponseEntity.ok(response);
    }

    private User getUser() {
        return iUserRepository.findByLogin(authService.getAuthInfo().getPrincipal().toString())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private Cart getCart(User user) {
        return iCartRepository.findByUserLoginWithLock(user.getLogin())
                .orElseThrow(() -> new NotFoundException("Cart not found"));
    }

    private void validateCart(Cart cart) {
        if (cart.getCartProducts() == null || cart.getCartProducts().isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }
    }

    private static class AvailabilityResult {
        List<OrderProduct> orderProducts = new ArrayList<>();
        List<Map<String, Object>> unavailableProducts = new ArrayList<>();
        boolean hasAvailableProducts = false;
    }

    private AvailabilityResult checkAvailability(Cart cart) {
        AvailabilityResult result = new AvailabilityResult();

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
                result.unavailableProducts.add(unavailableProduct);
            } else {
                result.hasAvailableProducts = true;

                OrderProduct orderProduct = OrderProduct.builder()
                        .part(partFromDb)
                        .partsCount(requestedCount)
                        .status(BoxStatus.ORDERED)
                        .price(partFromDb.getPrice().multiply(BigDecimal.valueOf(requestedCount)))
                        .currency(partFromDb.getCurrency())
                        .build();
                result.orderProducts.add(orderProduct);
            }
        }
        return result;
    }

    private BigDecimal calculateTotalOrderPrice(List<OrderProduct> orderProducts) {
        return orderProducts.stream()
                .map(OrderProduct::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Order buildAndSaveOrder(User user, OrderStatus status) {
        Long orderNumber = generateOrderNumber();
        Order order = Order.builder()
                .user(user)
                .number(orderNumber)
                .status(status)
                .build();
        return iOrderRepository.save(order);
    }

    private void saveOrderProducts(Order order, List<OrderProduct> orderProducts) {
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
    }

    private void processPayment(User user, Order order, BigDecimal amount) {
        BalanceOperationDto balanceOperation = new BalanceOperationDto();
        balanceOperation.setUserId(user.getId().toString());
        balanceOperation.setAmount(amount);
        balanceOperation.setOperationType(BalanceOperationType.PAYMENT);
        balanceOperation.setDescription("Payment for order #" + order.getNumber());
        balanceService.processBalance(balanceOperation);
    }

    /**
     * Updates order status (admin only)
     * @param id order ID
     * @param order the Order object containing updated fields
     * @return ResponseEntity with a success/error message
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
     * @return ResponseEntity with a success/error message
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
     * Update the order status if all order products were delivered
     * @param order order an object to update
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
     * Update order products status if the order was cancelled
     * @param order order an object to update
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
     * @return necessary status
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
