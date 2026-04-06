package by.baykulbackend.services.email;

import by.baykulbackend.database.dao.order.Order;
import by.baykulbackend.database.dao.order.OrderStatus;
import by.baykulbackend.database.dao.user.Localization;
import by.baykulbackend.database.dao.user.User;
import by.baykulbackend.services.finance.PriceService;
import by.baykulbackend.services.order.OrderStatusChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEmailService {
    private final EmailService emailService;
    private final PriceService priceService;

    @Value("${app.url.orders-path:/orders}")
    private String ordersPath;

    @Value("${app.office.address.eng}")
    private String officeAddressEng;

    @Value("${app.office.address.rus}")
    private String officeAddressRus;

    private static final Map<OrderStatus, Map<Localization, String>> SUBJECTS = Map.of(
        OrderStatus.CONFIRMATION_WAITING, Map.of(
            Localization.RUS, "Заказ №%d ожидает подтверждения",
            Localization.ENG, "Order #%d awaiting confirmation"
        ),
        OrderStatus.PAYMENT_WAITING, Map.of(
            Localization.RUS, "Заказ №%d ожидает оплаты",
            Localization.ENG, "Order #%d awaiting payment"
        ),
        OrderStatus.ORDERED, Map.of(
            Localization.RUS, "Заказ №%d оформлен",
            Localization.ENG, "Order #%d confirmed"
        ),
        OrderStatus.ON_WAY, Map.of(
            Localization.RUS, "Заказ №%d в пути",
            Localization.ENG, "Order #%d on the way"
        ),
        OrderStatus.IN_WAREHOUSE, Map.of(
            Localization.RUS, "Заказ №%d на складе",
            Localization.ENG, "Order #%d in warehouse"
        ),
        OrderStatus.READY_FOR_PICKUP, Map.of(
            Localization.RUS, "Заказ №%d готов к выдаче",
            Localization.ENG, "Order #%d ready for pickup"
        ),
        OrderStatus.COMPLETED, Map.of(
            Localization.RUS, "Заказ №%d выполнен",
            Localization.ENG, "Order #%d completed"
        ),
        OrderStatus.CANCELLED, Map.of(
            Localization.RUS, "Заказ №%d отменён",
            Localization.ENG, "Order #%d cancelled"
        )
    );

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderStatusChange(OrderStatusChangeEvent event) {
        Order order = event.getOrder();
        OrderStatus oldStatus = event.getOldStatus();
        OrderStatus newStatus = event.getNewStatus();

        if (oldStatus == newStatus) {
            return;
        }
        
        User user = order.getUser();

        if (StringUtils.isBlank(user.getEmail())) {
            log.warn("User {} has no email, skipping order status notification", user.getId());
            return;
        }
        
        Localization localization = user.getLocalization() != null ? user.getLocalization() : Localization.RUS;
        
        sendStatusEmail(order, newStatus, localization);
    }

    public void sendOrderCreatedEmail(Order order) {
        User user = order.getUser();
        
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("User {} has no email, skipping order created notification", user.getId());
            return;
        }

        sendStatusEmail(order, OrderStatus.CONFIRMATION_WAITING, user.getLocalization());
    }
    
    private void sendStatusEmail(Order order, OrderStatus status, Localization localization) {
        String templateName = getTemplateNameForStatus(status);
        String subjectTemplate = SUBJECTS.get(status).get(localization);
        String subject = String.format(subjectTemplate, order.getNumber());
        
        Context context = buildEmailContext(order, status, localization);
        
        emailService.sendEmail(
            order.getUser().getEmail(),
            subject,
            templateName,
            localization,
            context
        );
        
        log.info("Order status email sent: orderId={}, status={}, localization={}", 
            order.getId(), status, localization);
    }
    
    private String getTemplateNameForStatus(OrderStatus status) {
        return switch (status) {
            case CONFIRMATION_WAITING -> "order-confirmation-waiting";
            case PAYMENT_WAITING -> "order-payment-waiting";
            case ORDERED -> "order-ordered";
            case ON_WAY -> "order-on-way";
            case IN_WAREHOUSE -> "order-in-warehouse";
            case READY_FOR_PICKUP -> "order-ready-for-pickup";
            case COMPLETED -> "order-completed";
            case CANCELLED -> "order-cancelled";
        };
    }
    
    private Context buildEmailContext(Order order, OrderStatus status, Localization localization) {
        Context context = new Context();
        context.setVariable("localization", localization.name());
        context.setVariable("orderNumber", order.getNumber());
        context.setVariable("status", status.name());
        context.setVariable("userName",
                Optional.of(order.getUser().getProfile().getName()).orElse(order.getUser().getLogin()));
        context.setVariable("orderDate", formatDateTime(order.getCreatedTs(), localization));
        context.setVariable("url", emailService.getBaseUrl() + ordersPath + "/" + order.getId());

        BigDecimal totalAmount = calculateTotalAmount(order);
        context.setVariable("totalAmount", String.format("%.2f %s", totalAmount, priceService.getSystemCurrency()));

        addStatusSpecificVariables(context, status, localization);
        
        return context;
    }
    
    private String formatDateTime(LocalDateTime dateTime, Localization localization) {
        if (dateTime == null) return "";
        String pattern = localization == Localization.RUS 
            ? "dd.MM.yyyy HH:mm" 
            : "MM/dd/yyyy HH:mm";
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }
    
    private BigDecimal calculateTotalAmount(Order order) {
        if (order.getOrderProducts() == null || order.getOrderProducts().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return order.getOrderProducts().stream()
            .map(op -> op.getPrice().multiply(BigDecimal.valueOf(op.getPartsCount())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private void addStatusSpecificVariables(Context context, OrderStatus status, Localization localization) {
        switch (status) {
            case READY_FOR_PICKUP -> context.setVariable("pickupMessage", localization == Localization.RUS
                    ? "Ваш заказ можно забрать по адресу: " + officeAddressRus
                    : "Your order is ready for pickup at: " + officeAddressEng
            );
            case ON_WAY -> context.setVariable("trackingInfo", localization == Localization.RUS
                    ? "Отследить заказ можно в личном кабинете"
                    : "Track your order in your personal account"
            );
            case CANCELLED -> context.setVariable("cancellationMessage", localization == Localization.RUS
                    ? "Если вы не отменяли заказ, свяжитесь с поддержкой"
                    : "If you didn't cancel the order, please contact support");
            default -> {}
        }
    }
}