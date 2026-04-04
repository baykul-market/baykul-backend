package by.baykulbackend.services.order;

import by.baykulbackend.database.dao.order.Order;
import by.baykulbackend.database.dao.order.OrderStatus;
import lombok.Getter;

@Getter
public class OrderStatusChangeEvent {
    private final Order order;
    private final OrderStatus oldStatus;
    private final OrderStatus newStatus;

    public OrderStatusChangeEvent(Order order, OrderStatus oldStatus, OrderStatus newStatus) {
        this.order = order;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }
}