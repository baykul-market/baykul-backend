package by.baykulbackend.services.bill;

import by.baykulbackend.database.dao.bill.Bill;
import by.baykulbackend.database.dao.bill.BillStatus;
import by.baykulbackend.database.dao.order.BoxStatus;
import by.baykulbackend.database.dao.order.Order;
import by.baykulbackend.database.dao.order.OrderProduct;
import by.baykulbackend.database.dao.order.OrderStatus;
import by.baykulbackend.database.repository.bill.IBillRepository;
import by.baykulbackend.database.repository.order.IOrderProductRepository;
import by.baykulbackend.database.repository.order.IOrderRepository;
import by.baykulbackend.exceptions.BadRequestException;
import by.baykulbackend.exceptions.NotFoundException;
import by.baykulbackend.services.order.OrderService;
import by.baykulbackend.services.order.OrderStatusChangeEvent;
import by.baykulbackend.services.user.AuthService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service class for managing bills and their associated order products.
 * Provides functionality for creating, updating, applying, and deleting bills,
 * as well as managing order products within bills.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillService {
    private final IOrderProductRepository iOrderProductRepository;
    private final IOrderRepository iOrderRepository;
    private final IBillRepository iBillRepository;
    private final AuthService authService;
    private final OrderService orderService;

    private static final Long START_BILL_NUMBER = 10000L;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Creates a new bill with DRAFT status.
     * Optionally associates order products with the bill if they are available.
     *
     * @param bill The bill object containing optional order products to associate
     * @return ResponseEntity with creation status, bill ID, and any unavailable order products
     */
    @Transactional
    public ResponseEntity<?> createBill(Bill bill) {
        Map<String, Object> response = new HashMap<>();

        Bill newBill = new Bill();
        newBill.setStatus(BillStatus.DRAFT);
        newBill.setNumber(generateBillNumber());

        iBillRepository.save(newBill);

        List<OrderProduct> orderProducts = new ArrayList<>();

        if (bill.getOrderProducts() != null && !bill.getOrderProducts().isEmpty()) {
            List<UUID> unavailableOrderProductIds = new ArrayList<>();

            //noinspection SimplifyStreamApiCallChains
            Set<UUID> orderProductIdSet = bill.getOrderProducts().stream()
                    .filter(op -> op.getId() != null)
                    .map(OrderProduct::getId)
                    .collect(Collectors.toSet());

            for (UUID orderProductId : orderProductIdSet) {
                iOrderProductRepository.findByBillIsNullAndIdAndStatusIn(
                        orderProductId,
                        List.of(BoxStatus.ON_WAY, BoxStatus.TO_ORDER)
                ).ifPresentOrElse(
                        op -> {
                            op.setBill(newBill);
                            orderProducts.add(op);
                        },
                        () -> unavailableOrderProductIds.add(orderProductId)
                );
            }

            if (!unavailableOrderProductIds.isEmpty()) {
                response.put("unavailable_order_products", unavailableOrderProductIds);
            }
        }

        if (!orderProducts.isEmpty()) {
            iOrderProductRepository.saveAll(orderProducts);
        }

        response.put("create_bill", "true");
        response.put("id", newBill.getId().toString());

        log.info("Bill {} created with {} order products -> {}",
                newBill.getId(), orderProducts.size(), authService.getAuthInfo().getPrincipal());

        return ResponseEntity.ok(response);
    }

    /**
     * Applies a bill by changing its status from DRAFT to APPLIED.
     * Only bills in DRAFT status can be applied.
     *
     * @param billId UUID of the bill to apply
     * @return ResponseEntity with update status
     * @throws NotFoundException if bill not found
     * @throws BadRequestException if bill is not in DRAFT status
     */
    @Transactional
    public ResponseEntity<?> applyBill(UUID billId) {
        Map<String, Object> response = new HashMap<>();

        Bill billFromDb = iBillRepository.findById(billId)
                .orElseThrow(() -> new NotFoundException("Bill not found"));

        if (!billFromDb.getStatus().equals(BillStatus.DRAFT)) {
            throw new BadRequestException("Cannot update non-draft bill");
        }

        billFromDb.setStatus(BillStatus.APPLIED);
        billFromDb.getOrderProducts().forEach(op -> op.setStatus(BoxStatus.ARRIVED));

        iOrderProductRepository.saveAll(billFromDb.getOrderProducts());
        iBillRepository.save(billFromDb);

        List<Order> ordersToUpdate = billFromDb.getOrderProducts().stream().map(OrderProduct::getOrder).toList();
        Map<Order, OrderStatus> ordersToUpdateOldStatuses = ordersToUpdate.stream()
                .collect(Collectors.toMap(
                        order -> order,
                        Order::getStatus
                ));
        ordersToUpdate.forEach(orderService::updateOrderStatus);
        iOrderRepository.saveAll(ordersToUpdate);

        ordersToUpdateOldStatuses.forEach((order, oldStatus) ->
                eventPublisher.publishEvent(new OrderStatusChangeEvent(order, oldStatus, order.getStatus())));

        response.put("update_bill", "true");
        log.info("Bill {} applied -> {}", billId, authService.getAuthInfo().getPrincipal());

        return ResponseEntity.ok(response);
    }

    /**
     * Adds an order product to a draft bill.
     * Validates that the bill is in DRAFT status and the order product is not already
     * associated with any bill.
     *
     * @param billId UUID of the bill
     * @param orderProductId UUID of the order product to add
     * @return ResponseEntity with update status
     * @throws NotFoundException if bill or order product not found
     * @throws BadRequestException if bill is not in DRAFT status or order product is already in an applied bill
     */
    @Transactional
    public ResponseEntity<?> addBoxToBill(UUID billId, UUID orderProductId) {
        Map<String, Object> response = new HashMap<>();

        Bill bill = iBillRepository.findById(billId)
                .orElseThrow(() -> new NotFoundException("Bill not found"));

        if (!bill.getStatus().equals(BillStatus.DRAFT)) {
            throw new BadRequestException("Cannot update non-draft bill");
        }

        OrderProduct orderProduct = iOrderProductRepository.findById(orderProductId)
                .orElseThrow(() -> new NotFoundException("Order product not found"));

        if (orderProduct.getBill() != null) {
            throw new BadRequestException(
                    String.format("Order product is already in bill %s", orderProduct.getBill().getId())
            );
        }

        orderProduct.setBill(bill);
        iOrderProductRepository.save(orderProduct);
        response.put("update_bill", "true");
        log.info("Order product {} added to bill {} -> {}",
                orderProductId, bill.getId(), authService.getAuthInfo().getPrincipal());

        return ResponseEntity.ok(response);
    }

    /**
     * Removes an order product from a draft bill.
     * Validates that the order product is actually associated with the specified bill.
     *
     * @param billId UUID of the bill
     * @param orderProductId UUID of the order product to remove
     * @return ResponseEntity with update status
     * @throws NotFoundException if bill or order product not found
     * @throws BadRequestException if bill is not in DRAFT status or order product not in this bill
     */
    @Transactional
    public ResponseEntity<?> removeBoxFromBill(UUID billId, UUID orderProductId) {
        Map<String, Object> response = new HashMap<>();

        Bill bill = iBillRepository.findById(billId)
                .orElseThrow(() -> new NotFoundException("Bill not found"));

        if (!bill.getStatus().equals(BillStatus.DRAFT)) {
            throw new BadRequestException("Cannot update non-draft bill");
        }

        OrderProduct orderProduct = iOrderProductRepository.findById(orderProductId)
                .orElseThrow(() -> new NotFoundException("Order product not found"));

        if (orderProduct.getBill() == null || !orderProduct.getBill().equals(bill)) {
            throw new BadRequestException("Order product is not in that bill");
        }

        orderProduct.setBill(null);
        iOrderProductRepository.save(orderProduct);
        response.put("update_bill", "true");
        log.info("Order product {} removed from bill {} -> {}",
                orderProductId, bill.getId(), authService.getAuthInfo().getPrincipal());

        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a draft bill and detaches all associated order products.
     * Only bills in DRAFT status can be deleted.
     *
     * @param billId UUID of the bill to delete
     * @return ResponseEntity with deletion status
     * @throws NotFoundException if bill not found
     * @throws BadRequestException if bill is not in DRAFT status
     */
    @Transactional
    public ResponseEntity<?> deleteBillById(UUID billId) {
        Map<String, Object> response = new HashMap<>();

        Bill bill = iBillRepository.findById(billId)
                .orElseThrow(() -> new NotFoundException("Bill not found"));

        if (bill.getStatus() != BillStatus.DRAFT) {
            throw new BadRequestException("Can delete only draft bill");
        }

        // Detach all associated order products
        if (bill.getOrderProducts() != null) {
            for (OrderProduct orderProduct : bill.getOrderProducts()) {
                orderProduct.setBill(null);
            }
            iOrderProductRepository.saveAll(bill.getOrderProducts());
        }

        iBillRepository.delete(bill);
        response.put("delete_bill", "true");
        log.info("Bill {} deleted -> {}", bill.getId(), authService.getAuthInfo().getPrincipal());

        return ResponseEntity.ok(response);
    }

    /**
     * Generates a unique bill number.
     * Finds the maximum existing bill number and increments by 1.
     * If no bills exist, starts from START_BILL_NUMBER.
     *
     * @return generated unique bill number
     */
    private Long generateBillNumber() {
        Long maxNumber = iBillRepository.findAll().stream()
                .map(Bill::getNumber)
                .filter(Objects::nonNull)
                .max(Long::compare)
                .orElse(START_BILL_NUMBER - 1);

        return maxNumber + 1;
    }
}