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
import by.baykulbackend.database.model.Permission;
import by.baykulbackend.database.repository.cart.ICartProductRepository;
import by.baykulbackend.database.repository.cart.ICartRepository;
import by.baykulbackend.database.repository.order.IOrderProductRepository;
import by.baykulbackend.database.repository.order.IOrderRepository;
import by.baykulbackend.database.repository.product.IPartRepository;
import by.baykulbackend.database.repository.user.IUserRepository;
import by.baykulbackend.exceptions.BadRequestException;
import by.baykulbackend.exceptions.NotFoundException;
import by.baykulbackend.services.email.OrderEmailService;
import by.baykulbackend.services.finance.CurrencyExchangeService;
import by.baykulbackend.services.finance.PriceService;
import by.baykulbackend.services.user.AuthService;
import by.baykulbackend.database.dao.balance.BalanceOperationType;
import by.baykulbackend.services.balance.BalanceService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Collections;
import java.util.Objects;

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
    private final PriceService priceService;
    private final CurrencyExchangeService currencyExchangeService;
    private final OrderEmailService orderEmailService;
    private final ApplicationEventPublisher eventPublisher;

    public Order getMyById(UUID id) {
        Order order = iOrderRepository.findByUserLoginAndId(authService.getAuthInfo().getPrincipal().toString(), id)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (!getCurrentUser().getRole().getPermissions().contains(Permission.PRODUCT_WRITE)) {
            for (OrderProduct orderProduct : order.getOrderProducts()) {
                orderProduct.getPart().setPrice(null);
                orderProduct.getPart().setCurrency(null);
            }
        }

        return order;
    }

    public List<Order> getMyAll(Pageable pageable) {
        List<Order> orders = iOrderRepository.findByUserLogin(
                authService.getAuthInfo().getPrincipal().toString(),
                pageable
        ).stream().toList();

        if (!getCurrentUser().getRole().getPermissions().contains(Permission.PRODUCT_WRITE)) {
            for (Order order : orders) {
                for (OrderProduct orderProduct : order.getOrderProducts()) {
                    orderProduct.getPart().setPrice(null);
                    orderProduct.getPart().setCurrency(null);
                }
            }
        }

        return orders;
    }

    /**
     * Creates a new order from the user's cart.
     * <p>
     * Process flow:
     * 1. Validates cart is not empty
     * 2. Generates unique order number
     * 3. Sets initial status based on user's pay-later capability
     * 4. Creates order products with CREATED status
     * 5. Clears the user's cart
     *
     * @return ResponseEntity with creation status and order ID
     * @throws NotFoundException if user or cart not found
     * @throws BadRequestException if cart is empty
     */
    @Transactional
    public ResponseEntity<?> createOrderFromCart() {
        Map<String, Object> response = new HashMap<>();

        User user = getCurrentUser();
        Cart cart = iCartRepository.findByUserLoginWithLock(user.getLogin())
                .orElseThrow(() -> new NotFoundException("Cart not found"));

        if (cart.getCartProducts() == null || cart.getCartProducts().isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        Order order = buildAndSaveOrder(user);
        saveOrderProducts(order, cart);

        iCartProductRepository.deleteAllByCartId(cart.getId());

        response.put("create_order", "true");
        response.put("id", order.getId().toString());

        log.info("Order {} created for user {} -> {}", order.getNumber(), user.getLogin(),
                authService.getAuthInfo().getPrincipal());

        orderEmailService.sendOrderCreatedEmail(order);

        return ResponseEntity.ok(response);
    }

    /**
     * Confirms an order that is waiting for confirmation.
     * <p>
     * This operation:
     * 1. Validates order is in CONFIRMATION_WAITING status
     * 2. Changes status to ORDERED
     * 3. Updates product statuses based on availability:
     *    - Products with sufficient stock → IN_WAREHOUSE
     *    - Products with insufficient stock → TO_ORDER
     * 4. Updates overall order status based on product statuses
     *
     * @param orderId UUID of the order to confirm
     * @return ResponseEntity with confirmation status
     * @throws NotFoundException if order not found
     * @throws BadRequestException if order is not in CONFIRMATION_WAITING status
     */
    @Transactional
    public ResponseEntity<?> confirmOrder(UUID orderId) {
        Map<String, Object> response = new HashMap<>();

        Order order = iOrderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (!order.getStatus().equals(OrderStatus.CONFIRMATION_WAITING)) {
            throw new BadRequestException("No confirmation needed");
        }

        updateOrderStatusAfterConfirmation(order);
        response.put("confirmation", "true");
        log.info("Order {} confirmed by {}", order.getNumber(), authService.getAuthInfo().getPrincipal());

        return ResponseEntity.ok(response);
    }

    /**
     * Pays for an existing order.
     *
     * @param orderId The UUID of the order to pay for
     * @return ResponseEntity with payment status
     * @throws NotFoundException if order not found
     * @throws BadRequestException if order is already paid
     */
    @Transactional
    public ResponseEntity<?> payForOrder(UUID orderId) {
        Map<String, Object> response = new HashMap<>();

        Order order = iOrderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        return payForOrder(order, response);
    }

    /**
     * Pays for an existing order belonging to the current user.
     *
     * @param orderId The UUID of the order to pay for
     * @return ResponseEntity with payment status
     * @throws NotFoundException if order not found
     * @throws BadRequestException if order doesn't belong to user or is already paid
     */
    @Transactional
    public ResponseEntity<?> payForUsersOrder(UUID orderId) {
        Map<String, Object> response = new HashMap<>();

        User user = getCurrentUser();
        Order order = iOrderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Order does not belong to the current user");
        }

        return payForOrder(order, response);
    }

    /**
     * Updates an order product with new values.
     * <p>
     * Validates and applies updates to:
     * - Status (must follow transition rules)
     * - Number (must be >= 100000 and unique)
     * - Parts count (must be positive)
     * <p>
     * After status update, automatically updates parent order status
     * based on all products' statuses.
     *
     * @param id order product ID
     * @param orderProduct the Order product object containing updated fields
     * @return ResponseEntity with update status
     * @throws NotFoundException if order product not found
     * @throws BadRequestException if validation fails
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
            if (!isTransitionAllowed(orderProductFromDb.getStatus(), orderProduct.getStatus())) {
                throw new BadRequestException("Order product's status transition not allowed");
            }

            if (orderProduct.getStatus().equals(BoxStatus.DELIVERED) && !orderProductFromDb.getOrder().getPaid()) {
                throw new BadRequestException("Order is not paid");
            }

            orderProductFromDb.setStatus(orderProduct.getStatus());
            log.info("Order product's status with id {} has been updated to {} -> {}",
                    id, orderProduct.getStatus(), authService.getAuthInfo().getPrincipal());
        }

        if (orderProduct.getNumber() != null && !orderProduct.getNumber().equals(orderProductFromDb.getNumber())) {
            orderProductFromDb.setNumber(orderProduct.getNumber());
            log.info("Order product's number with id {} has been updated to {} -> {}",
                    id, orderProduct.getNumber(), authService.getAuthInfo().getPrincipal());
        }

        if (orderProduct.getPartsCount() != null
                && !orderProduct.getPartsCount().equals(orderProductFromDb.getPartsCount())) {
            orderProductFromDb.setPartsCount(orderProduct.getPartsCount());
            log.info("Order product's part's count with id {} has been updated to {} -> {}",
                    id, orderProduct.getPartsCount(), authService.getAuthInfo().getPrincipal());
        }

        iOrderProductRepository.save(orderProductFromDb);
        response.put("update_order_product", "true");

        if (orderProduct.getStatus() != null) {
            OrderStatus oldStatus = orderProductFromDb.getOrder().getStatus();
            updateOrderStatus(orderProductFromDb.getOrder());
            iOrderRepository.save(orderProductFromDb.getOrder());
            eventPublisher.publishEvent(new OrderStatusChangeEvent(
                    orderProductFromDb.getOrder(), oldStatus, orderProductFromDb.getOrder().getStatus()
            ));
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Completes an order that is ready for pickup.
     * <p>
     * Requirements:
     * - Order must be in READY_FOR_PICKUP status
     * - Order must be paid
     * <p>
     * Effects:
     * - Order status becomes COMPLETED
     * - All order products become DELIVERED
     *
     * @param orderId UUID of the order to complete
     * @return ResponseEntity with completion status
     * @throws NotFoundException if order not found
     * @throws BadRequestException if order is not ready or not paid
     */
    @Transactional
    public ResponseEntity<?> completeOrder(UUID orderId) {
        Map<String, Object> response = new HashMap<>();

        Order order = iOrderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (!order.getStatus().equals(OrderStatus.READY_FOR_PICKUP)) {
            throw new BadRequestException("Completing order is not allowed: it is not ready");
        }

        if (!order.getPaid()) {
            throw new BadRequestException("Completing order is not allowed: it is not paid");
        }

        order.setStatus(OrderStatus.COMPLETED);
        updateOrderProductsStatus(order);

        iOrderRepository.save(order);
        iOrderProductRepository.saveAll(order.getOrderProducts());
        response.put("complete_order", "true");
        log.info("Order {} was completed", order.getNumber());

        eventPublisher.publishEvent(new OrderStatusChangeEvent(order, OrderStatus.READY_FOR_PICKUP, OrderStatus.COMPLETED));

        return ResponseEntity.ok(response);
    }

    /**
     * Cancels an order belonging to the current user.
     * <p>
     * User cancellation is only allowed for orders in:
     * - CONFIRMATION_WAITING
     * - PAYMENT_WAITING
     *
     * @param orderId UUID of the order to cancel
     * @return ResponseEntity with cancellation status
     * @throws NotFoundException if order not found
     * @throws BadRequestException if order doesn't belong to user or cannot be cancelled
     */
    @Transactional
    public ResponseEntity<?> cancelUsersOrder(UUID orderId) {
        Map<String, Object> response = new HashMap<>();

        User user = getCurrentUser();
        Order order = iOrderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Order does not belong to the current user");
        }

        if (!Set.of(OrderStatus.CONFIRMATION_WAITING, OrderStatus.PAYMENT_WAITING).contains(order.getStatus())) {
            throw new BadRequestException("Cancelling order is not allowed");
        }

        return cancelOrder(order, response);
    }

    /**
     * Cancels an order at any stage.
     *
     * @param orderId UUID of the order to cancel
     * @return ResponseEntity with cancellation status
     * @throws NotFoundException if order not found
     */
    @Transactional
    public ResponseEntity<?> cancelOrder(UUID orderId) {
        Map<String, Object> response = new HashMap<>();

        Order order = iOrderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        return cancelOrder(order, response);
    }

    /**
     * Updates the order status based on its products' statuses.
     * <p>
     * Status determination rules:
     * - All products CANCELLED → Order CANCELLED
     * - All products >= SHIPPED → Order READY_FOR_PICKUP
     * - All products >= ARRIVED → Order IN_WAREHOUSE
     * - Any product ON_WAY → Order ON_WAY
     * <p>
     * This method only updates the status object without saving to database.
     *
     * @param order order object to update
     */
    public void updateOrderStatus(Order order) {
        List<OrderProduct> orderProducts = order.getOrderProducts();

        boolean allCanceled = orderProducts.stream()
                .allMatch(op -> op.getStatus().equals(BoxStatus.CANCELLED));
        boolean allDelivered = orderProducts.stream()
                .allMatch(op -> op.getStatus().compare(BoxStatus.DELIVERED) >= 0);
        boolean allShipped = orderProducts.stream()
                .allMatch(op -> op.getStatus().compare(BoxStatus.SHIPPED) >= 0);
        boolean allInWarehouse = orderProducts.stream()
                .allMatch(op -> op.getStatus().compare(BoxStatus.ARRIVED) >= 0);
        boolean anyOnWay = orderProducts.stream()
                .anyMatch(op -> op.getStatus().equals(BoxStatus.ON_WAY));

        if (allCanceled) {
            order.setStatus(OrderStatus.CANCELLED);
        }
        else if (allDelivered) {
            order.setStatus(OrderStatus.COMPLETED);
        }
        else if (allShipped) {
            order.setStatus(OrderStatus.READY_FOR_PICKUP);
        }
        else if (allInWarehouse) {
            order.setStatus(OrderStatus.IN_WAREHOUSE);
        }
        else if (anyOnWay) {
            order.setStatus(OrderStatus.ON_WAY);
        }
    }

    @Transactional
    public Page<OrderProduct> searchOrderProducts(Long number, BoxStatus status, Boolean forBill, Pageable pageable) {
        boolean hasNumber = number != null;
        boolean hasStatus = status != null;
        forBill = forBill != null ? forBill : false;

        if (hasNumber && hasStatus && forBill) {
            if (BoxStatus.REQUIRED_FOR_BILL_CREATION.contains(status)) {
                return iOrderProductRepository.findAllByBillIsNullAndStatusAndNumberStartingWith(
                        status,
                        number.toString(),
                        pageable
                );
            } else {
                return new PageImpl<>(Collections.emptyList());
            }
        }
        else if (hasNumber && hasStatus) {
            return iOrderProductRepository.findAllByNumberStartingWithAndStatus(number.toString(), status, pageable);
        }
        else if (hasNumber && forBill) {
            return iOrderProductRepository.findAllByBillIsNullAndStatusInAndNumberStartingWith(
                    BoxStatus.REQUIRED_FOR_BILL_CREATION,
                    number.toString(),
                    pageable
            );
        }
        else if (hasStatus && forBill) {
            if (BoxStatus.REQUIRED_FOR_BILL_CREATION.contains(status)) {
                return iOrderProductRepository.findAllByStatusAndBillIsNull(status, pageable);
            } else {
                return new PageImpl<>(Collections.emptyList());
            }
        }
        else if (hasNumber) {
            return iOrderProductRepository.findAllByNumberStartingWith(number.toString(), pageable);
        }
        else if (hasStatus) {
            return iOrderProductRepository.findAllByStatus(status, pageable);
        }
        else if (forBill) {
            return iOrderProductRepository.findAllByBillIsNullAndStatusIn(BoxStatus.REQUIRED_FOR_BILL_CREATION, pageable);
        }

        return iOrderProductRepository.findAll(pageable);
    }

    /**
     * Builds and saves a new order with generated number and initial status.
     *
     * @param user the user who owns the order
     * @return saved Order entity
     */
    private Order buildAndSaveOrder(User user) {
        Long orderNumber = generateOrderNumber();

        OrderStatus status = user.getCanPayLater() ? OrderStatus.CONFIRMATION_WAITING : OrderStatus.PAYMENT_WAITING;

        Order order = Order.builder()
                .user(user)
                .number(orderNumber)
                .status(status)
                .paid(false)
                .build();
        return iOrderRepository.save(order);
    }

    /**
     * Creates and saves order products from cart items.
     *
     * @param order the order to associate products with
     * @param cart the cart containing the products
     */
    private void saveOrderProducts(Order order, Cart cart) {
        List<OrderProduct> orderProducts = new ArrayList<>();

        for (CartProduct cartProduct : cart.getCartProducts()) {
            boolean needsDelivery = cartProduct.getPart().getStorageCount() == null
                    || cartProduct.getPartsCount() > cartProduct.getPart().getStorageCount();

            OrderProduct orderProduct = OrderProduct.builder()
                    .order(order)
                    .part(cartProduct.getPart())
                    .partsCount(cartProduct.getPartsCount())
                    .status(BoxStatus.CREATED)
                    .price(priceService.calculateProductPrice(cartProduct.getPart(), needsDelivery))
                    .currency(priceService.getSystemCurrency())
                    .build();

            orderProducts.add(orderProduct);
        }

        iOrderProductRepository.saveAll(orderProducts);
    }

    /**
     * Checks if a transition from one BoxStatus to another is allowed.
     * <p>
     * Allowed transitions:
     * - TO_ORDER → ON_WAY (ordered from supplier)
     * - ARRIVED/IN_WAREHOUSE → SHIPPED (shipped to customer)
     * - CREATED → CANCELLED (cancel before processing)
     * - DELIVERED → RETURNED (customer return)
     * - SHIPPED → DELIVERED (delivery confirmed)
     *
     * @param from current status
     * @param to target status
     * @return true if transition is allowed, false otherwise
     */
    private boolean isTransitionAllowed(BoxStatus from, BoxStatus to) {
        boolean isOrderingTransition = from.equals(BoxStatus.TO_ORDER) && to.equals(BoxStatus.ON_WAY);

        boolean isShippingTransition = (from.equals(BoxStatus.ARRIVED) || from.equals(BoxStatus.IN_WAREHOUSE))
                && to.equals(BoxStatus.SHIPPED);

        boolean isCancellingTransition = from.equals(BoxStatus.CREATED) && to.equals(BoxStatus.CANCELLED);

        boolean isReturningTransition = from.equals(BoxStatus.DELIVERED) && to.equals(BoxStatus.RETURNED);

        boolean isDeliveringTransition = from.equals(BoxStatus.SHIPPED) && to.equals(BoxStatus.DELIVERED);

        return isOrderingTransition || isShippingTransition || isCancellingTransition || isReturningTransition
                || isDeliveringTransition;
    }

    /**
     * Retrieves the currently authenticated user.
     *
     * @return User entity
     * @throws NotFoundException if user not found
     */
    private User getCurrentUser() {
        return iUserRepository.findByLogin(authService.getAuthInfo().getPrincipal().toString())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    /**
     * Updates order products statuses based on order status changes.
     * <p>
     * Status mappings:
     * - ORDERED → Check availability: TO_ORDER or IN_WAREHOUSE
     * - COMPLETED → DELIVERED
     * - CANCELLED → CANCELLED
     *
     * @param order the order whose products need updating
     */
    private void updateOrderProductsStatus(Order order) {
        for (OrderProduct orderProduct : order.getOrderProducts()) {
            switch (order.getStatus()) {
                case ORDERED:
                    Integer requestedCount = orderProduct.getPartsCount();
                    Integer storageCount = orderProduct.getPart().getStorageCount();

                    if (storageCount == null || requestedCount > storageCount) {
                        orderProduct.setStatus(BoxStatus.TO_ORDER);
                    } else {
                        orderProduct.setStatus(BoxStatus.IN_WAREHOUSE);

                        Part part = orderProduct.getPart();
                        part.setStorageCount(storageCount - requestedCount);
                        iPartRepository.save(part);
                    }
                    break;

                case COMPLETED:
                    orderProduct.setStatus(BoxStatus.DELIVERED);
                    break;

                case CANCELLED:
                    orderProduct.setStatus(BoxStatus.CANCELLED);
                    break;

                default:
                    break;
            }
        }
    }

    /**
     * Processes payment for an order by deducting amount from user's balance.
     *
     * @param order the order being paid for
     * @param amount the payment amount
     */
    private void processPayment(Order order, BigDecimal amount) {
        BalanceOperationDto balanceOperation = new BalanceOperationDto();
        balanceOperation.setUserId(order.getUser().getId().toString());
        balanceOperation.setAmount(amount);
        balanceOperation.setCurrency(priceService.getSystemCurrency());
        balanceOperation.setOperationType(BalanceOperationType.PAYMENT);
        balanceOperation.setDescription("Payment for order № " + order.getNumber());

        balanceService.processBalance(balanceOperation);
    }

    /**
     * Updates order status after confirmation or payment.
     * <p>
     * This method:
     * 1. Changes status from waiting state to ORDERED
     * 2. Updates product statuses based on availability
     * 3. Recalculates overall order status
     *
     * @param order the order to update
     */
    private void updateOrderStatusAfterConfirmation(Order order) {
        if (Set.of(OrderStatus.CONFIRMATION_WAITING, OrderStatus.PAYMENT_WAITING).contains(order.getStatus())) {
            OrderStatus oldStatus = order.getStatus();

            order.setStatus(OrderStatus.ORDERED);
            updateOrderProductsStatus(order);
            updateOrderStatus(order);

            iOrderProductRepository.saveAll(order.getOrderProducts());
            iOrderRepository.save(order);

            eventPublisher.publishEvent(new OrderStatusChangeEvent(order, oldStatus, OrderStatus.ORDERED));
        }
    }

    /**
     * Pays for an existing order.
     *
     * @param order The order to pay for
     * @param response Map to collect validation error messages
     * @return ResponseEntity with success/error message
     * @throws BadRequestException if order is already paid
     */
    private ResponseEntity<?> payForOrder(Order order, Map<String, Object> response) {
        if (order.getPaid()) {
            throw new BadRequestException("Order is already paid");
        }

        BigDecimal totalOrderPrice = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (OrderProduct orderProduct : order.getOrderProducts()) {
            totalOrderPrice = totalOrderPrice.add(
                    currencyExchangeService.exchange(
                            orderProduct.getPrice(), orderProduct.getCurrency(), priceService.getSystemCurrency()
                    ).multiply(BigDecimal.valueOf(orderProduct.getPartsCount()))
            );
        }

        processPayment(order, totalOrderPrice);
        order.setPaid(true);
        updateOrderStatusAfterConfirmation(order);

        response.put("pay_order", "true");

        return ResponseEntity.ok(response);
    }

    /**
     * Cancels an order.
     *
     * @param order the order to cancel
     * @param response Map to collect response messages
     * @return ResponseEntity with cancellation status
     */
    private ResponseEntity<?> cancelOrder(Order order, Map<String, Object> response) {
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        updateOrderProductsStatus(order);

        iOrderRepository.save(order);
        iOrderProductRepository.saveAll(order.getOrderProducts());
        response.put("cancel_order", "true");
        log.info("Order {} was cancelled", order.getNumber());

        eventPublisher.publishEvent(new OrderStatusChangeEvent(order, oldStatus, OrderStatus.CANCELLED));

        return ResponseEntity.ok(response);
    }

    /**
     * Generates a unique order number.
     * Finds the maximum existing order number and increments by 1.
     * If no orders exist, starts from START_ORDER_NUMBER.
     *
     * @return generated unique order number
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