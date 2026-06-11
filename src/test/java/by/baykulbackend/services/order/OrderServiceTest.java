package by.baykulbackend.services.order;

import by.baykulbackend.database.dao.order.BoxStatus;
import by.baykulbackend.database.dao.order.Order;
import by.baykulbackend.database.dao.order.OrderProduct;
import by.baykulbackend.database.dao.order.OrderStatus;
import by.baykulbackend.database.repository.order.IOrderProductRepository;
import by.baykulbackend.database.repository.order.IOrderRepository;
import by.baykulbackend.exceptions.BadRequestException;
import by.baykulbackend.security.JwtAuthentication;
import by.baykulbackend.services.user.AuthService;
import by.baykulbackend.services.email.OrderEmailService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private IOrderRepository iOrderRepository;
    @Mock
    private IOrderProductRepository iOrderProductRepository;
    @Mock
    private AuthService authService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private OrderEmailService orderEmailService;

    @InjectMocks
    private OrderService orderService;

    private Order order;
    private OrderProduct orderProduct;
    private UUID orderProductId;
    private JwtAuthentication authInfo;

    @BeforeEach
    void setUp() {
        orderProductId = UUID.randomUUID();

        order = new Order();
        order.setId(UUID.randomUUID());
        order.setPaid(false);
        order.setStatus(OrderStatus.ORDERED);

        orderProduct = new OrderProduct();
        orderProduct.setId(orderProductId);
        orderProduct.setOrder(order);
        orderProduct.setStatus(BoxStatus.IN_WAREHOUSE);
        orderProduct.setPaid(false);

        order.setOrderProducts(java.util.List.of(orderProduct));

        authInfo = new JwtAuthentication();
        authInfo.setLogin("testAdmin");
    }

    @Test
    void transitionToShippedShouldThrowExceptionIfOrderNotPaid() {
        when(iOrderProductRepository.findById(orderProductId)).thenReturn(Optional.of(orderProduct));

        OrderProduct patch = new OrderProduct();
        patch.setStatus(BoxStatus.SHIPPED);

        assertThrows(BadRequestException.class, () -> orderService.updateOrderProduct(orderProductId, patch));
    }

    @Test
    void transitionToShippedShouldSucceedIfOrderIsPaid() {
        orderProduct.setPaid(true);
        when(authService.getAuthInfo()).thenReturn(authInfo);
        when(iOrderProductRepository.findById(orderProductId)).thenReturn(Optional.of(orderProduct));

        OrderProduct patch = new OrderProduct();
        patch.setStatus(BoxStatus.SHIPPED);

        ResponseEntity<?> response = orderService.updateOrderProduct(orderProductId, patch);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(BoxStatus.SHIPPED, orderProduct.getStatus());
        verify(iOrderProductRepository).save(orderProduct);
    }

    @Test
    void transitionToDeliveredShouldSucceedIfOrderIsPaid() {
        orderProduct.setPaid(true);
        orderProduct.setStatus(BoxStatus.SHIPPED);
        when(authService.getAuthInfo()).thenReturn(authInfo);
        when(iOrderProductRepository.findById(orderProductId)).thenReturn(Optional.of(orderProduct));

        OrderProduct patch = new OrderProduct();
        patch.setStatus(BoxStatus.DELIVERED);

        ResponseEntity<?> response = orderService.updateOrderProduct(orderProductId, patch);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(BoxStatus.DELIVERED, orderProduct.getStatus());
        verify(iOrderProductRepository).save(orderProduct);
    }

    @Test
    void updatePriceShouldThrowIfPaid() {
        orderProduct.setPaid(true);
        when(iOrderProductRepository.findById(orderProductId)).thenReturn(Optional.of(orderProduct));

        OrderProduct patch = new OrderProduct();
        patch.setPrice(java.math.BigDecimal.TEN);

        assertThrows(BadRequestException.class, () -> orderService.updateOrderProduct(orderProductId, patch));
    }

    @Test
    void updatePriceShouldThrowIfNegative() {
        orderProduct.setPaid(false);
        when(iOrderProductRepository.findById(orderProductId)).thenReturn(Optional.of(orderProduct));

        OrderProduct patch = new OrderProduct();
        patch.setPrice(new java.math.BigDecimal("-5.00"));

        assertThrows(BadRequestException.class, () -> orderService.updateOrderProduct(orderProductId, patch));
    }

    @Test
    void updatePriceShouldSucceedIfUnpaid() {
        orderProduct.setPaid(false);
        orderProduct.setPrice(java.math.BigDecimal.ONE);
        orderProduct.setCurrency(by.baykulbackend.database.dao.finance.Currency.EUR);
        when(authService.getAuthInfo()).thenReturn(authInfo);
        when(iOrderProductRepository.findById(orderProductId)).thenReturn(Optional.of(orderProduct));

        OrderProduct patch = new OrderProduct();
        patch.setPrice(java.math.BigDecimal.TEN);

        ResponseEntity<?> response = orderService.updateOrderProduct(orderProductId, patch);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(java.math.BigDecimal.TEN, orderProduct.getPrice());
        verify(iOrderProductRepository).save(orderProduct);
        verify(orderEmailService).sendBoxPriceChangedEmail(orderProduct, java.math.BigDecimal.ONE, java.math.BigDecimal.TEN);
    }

}
