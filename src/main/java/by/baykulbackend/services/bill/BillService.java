package by.baykulbackend.services.bill;

import by.baykulbackend.database.dao.bill.Bill;
import by.baykulbackend.database.dao.bill.BillStatus;
import by.baykulbackend.database.dao.order.BoxStatus;
import by.baykulbackend.database.dao.order.OrderProduct;
import by.baykulbackend.database.repository.bill.IBillRepository;
import by.baykulbackend.database.repository.order.IOrderProductRepository;
import by.baykulbackend.exceptions.BadRequestException;
import by.baykulbackend.exceptions.NotFoundException;
import by.baykulbackend.services.user.AuthService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillService {
    private final IOrderProductRepository iOrderProductRepository;
    private final IBillRepository iBillRepository;
    private final AuthService authService;

    private static final Long START_BILL_NUMBER = 10000L;

    @Transactional
    public ResponseEntity<?> createBill(Bill bill) {
        Map<String, Object> response = new HashMap<>();

        Bill newBill = new Bill();
        newBill.setStatus(BillStatus.DRAFT);
        newBill.setNumber(generateBillNumber());

        List<OrderProduct> orderProducts = null;

        if (bill.getOrderProducts() != null) {
            orderProducts = new ArrayList<>();
            List<UUID> unavailableOrderProductIds = new ArrayList<>();

            //noinspection SimplifyStreamApiCallChains
            Set<UUID> orderProductIdSet = bill.getOrderProducts().stream()
                    .filter(op -> op.getId() != null)
                    .map(OrderProduct::getId)
                    .collect(Collectors.toSet());

            for (UUID orderProductId : orderProductIdSet) {
                iOrderProductRepository.findByBillIsNullAndId(orderProductId).ifPresentOrElse(
                        op -> op.setBill(newBill),
                        () -> unavailableOrderProductIds.add(orderProductId)
                );
            }

            if (unavailableOrderProductIds.isEmpty()) {
                response.put("unavailable_order_products", unavailableOrderProductIds);
            }
        }

        newBill.setOrderProducts(orderProducts);
        iBillRepository.save(newBill);

        if (orderProducts != null) {
            iOrderProductRepository.saveAll(orderProducts);
        }

        response.put("create_bill", "true");
        response.put("id", newBill.getId().toString());

        log.info("Bill {} created -> {}", newBill.getId(), authService.getAuthInfo().getPrincipal());

        return ResponseEntity.ok(response);
    }

    @Transactional
    public ResponseEntity<?> applyBill(UUID billId) {
        Map<String, Object> response = new HashMap<>();

        Bill billFromDb = iBillRepository.findById(billId)
                .orElseThrow(() -> new NotFoundException("Bill not found"));

        if (!billFromDb.getStatus().equals(BillStatus.DRAFT)) {
            throw new BadRequestException("Cannot update non-draft bill");
        }

        billFromDb.setStatus(BillStatus.APPLIED);
        iBillRepository.save(billFromDb);
        response.put("update_bill", "true");
        log.info("Bill {} updated -> {}", billId, authService.getAuthInfo().getPrincipal());

        return ResponseEntity.ok(response);
    }

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

        if (orderProduct.getBill() != null && orderProduct.getBill().getStatus().equals(BillStatus.APPLIED)) {
            throw new BadRequestException(
                    String.format("Order product is already in bill %s", orderProduct.getBill().getId())
            );
        }

        orderProduct.setBill(bill);
        iOrderProductRepository.save(orderProduct);
        response.put("update_bill", "true");
        log.info("Bill {} updated -> {}", bill.getId(), authService.getAuthInfo().getPrincipal());

        return ResponseEntity.ok(response);
    }

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
        log.info("Bill {} updated -> {}", bill.getId(), authService.getAuthInfo().getPrincipal());

        return ResponseEntity.ok(response);
    }

    @Transactional
    public ResponseEntity<?> deleteBillById(UUID billId) {
        Map<String, Object> response = new HashMap<>();

        Bill bill = iBillRepository.findById(billId).orElseThrow(() -> new NotFoundException("Bill not found"));

        if (bill.getStatus() != BillStatus.DRAFT) {
            throw new BadRequestException("Can delete only draft bill");
        }

        for (OrderProduct orderProduct : bill.getOrderProducts()) {
            orderProduct.setBill(null);
        }

        iOrderProductRepository.saveAll(bill.getOrderProducts());
        iBillRepository.delete(bill);
        response.put("delete_bill", "true");
        log.info("Bill {} deleted -> {}", bill.getId(), authService.getAuthInfo().getPrincipal());

        return ResponseEntity.ok(response);
    }

    /**
     * Generates unique bill number
     * @return generated bill number
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
