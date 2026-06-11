package by.baykulbackend.services.balance;

import by.baykulbackend.database.dao.balance.Balance;
import by.baykulbackend.database.dao.balance.BalanceHistory;
import by.baykulbackend.database.dao.finance.Currency;
import by.baykulbackend.database.dao.order.BoxStatus;
import by.baykulbackend.database.dao.order.OrderProduct;
import by.baykulbackend.database.dao.user.User;
import by.baykulbackend.database.dto.balance.BalanceOperationDto;
import by.baykulbackend.database.dao.balance.BalanceOperationType;
import by.baykulbackend.database.repository.balance.IBalanceRepository;
import by.baykulbackend.database.repository.order.IOrderProductRepository;
import by.baykulbackend.exceptions.BadRequestException;
import by.baykulbackend.security.JwtAuthentication;
import by.baykulbackend.services.finance.CurrencyExchangeService;
import by.baykulbackend.services.finance.PriceService;
import by.baykulbackend.services.user.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

    @Mock
    private IBalanceRepository iBalanceRepository;
    @Mock
    private AuthService authService;
    @Mock
    private CurrencyExchangeService currencyExchangeService;
    @Mock
    private IOrderProductRepository iOrderProductRepository;
    @Mock
    private PriceService priceService;

    @InjectMocks
    private BalanceService balanceService;

    private Balance balance;
    private User user;
    private JwtAuthentication authInfo;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setLogin("testUser");

        balance = new Balance();
        balance.setId(UUID.randomUUID());
        balance.setUser(user);
        balance.setAccount(new BigDecimal("100.00"));
        balance.setCurrency(Currency.RUB);
        balance.setBalanceHistoryList(new ArrayList<>());

        authInfo = new JwtAuthentication();
        authInfo.setLogin("testUser");
    }

    @Test
    void processPaymentShouldAllowNegativeBalance() {
        when(authService.getAuthInfo()).thenReturn(authInfo);
        when(iBalanceRepository.findByUserIdWithLock(user.getId())).thenReturn(Optional.of(balance));
        when(currencyExchangeService.exchange(any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        BalanceOperationDto operation = new BalanceOperationDto();
        operation.setUserId(user.getId().toString());
        operation.setAmount(new BigDecimal("150.00"));
        operation.setCurrency(Currency.RUB);
        operation.setOperationType(BalanceOperationType.PAYMENT);
        operation.setDescription("Order Payment");

        balanceService.processBalance(operation);

        assertEquals(new BigDecimal("-50.00"), balance.getAccount());
        verify(iBalanceRepository).save(balance);
    }

    @Test
    void processWithdrawalShouldThrowExceptionIfInsufficientFunds() {
        when(authService.getAuthInfo()).thenReturn(authInfo);
        when(iBalanceRepository.findByUserIdWithLock(user.getId())).thenReturn(Optional.of(balance));
        when(currencyExchangeService.exchange(any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        BalanceOperationDto operation = new BalanceOperationDto();
        operation.setUserId(user.getId().toString());
        operation.setAmount(new BigDecimal("150.00"));
        operation.setCurrency(Currency.RUB);
        operation.setOperationType(BalanceOperationType.WITHDRAWAL);
        operation.setDescription("Manual Withdrawal");

        assertThrows(BadRequestException.class, () -> balanceService.processBalance(operation));
    }

    @Test
    void calculateUnpaidSpendingsSumShouldAccumulateConvertedPrices() {
        OrderProduct op1 = new OrderProduct();
        op1.setPrice(new BigDecimal("20.00"));
        op1.setCurrency(Currency.RUB);
        op1.setPartsCount(2);

        OrderProduct op2 = new OrderProduct();
        op2.setPrice(new BigDecimal("10.00"));
        op2.setCurrency(Currency.EUR);
        op2.setPartsCount(3);

        List<OrderProduct> unpaidList = List.of(op1, op2);

        when(iOrderProductRepository.findAllUnpaidProductsByUserId(user.getId(), List.of(BoxStatus.CANCELLED, BoxStatus.RETURNED)))
                .thenReturn(unpaidList);
        when(priceService.getSystemCurrency()).thenReturn(Currency.RUB);
        
        // Mocking currency exchanges: 20 RUB -> 20 RUB, 10 EUR -> 100 RUB
        when(currencyExchangeService.exchange(new BigDecimal("20.00"), Currency.RUB, Currency.RUB))
                .thenReturn(new BigDecimal("20.00"));
        when(currencyExchangeService.exchange(new BigDecimal("10.00"), Currency.EUR, Currency.RUB))
                .thenReturn(new BigDecimal("100.00"));

        // Expected spendings: (20 * 2) + (100 * 3) = 40 + 300 = 340
        BigDecimal result = balanceService.calculateUnpaidSpendingsSum(user.getId());
        assertEquals(new BigDecimal("340.00"), result);
    }

    @Test
    void enrichBalanceShouldCalculateAndSetProjectedAccount() {
        OrderProduct op = new OrderProduct();
        op.setPrice(new BigDecimal("40.00"));
        op.setCurrency(Currency.RUB);
        op.setPartsCount(1);

        when(iOrderProductRepository.findAllUnpaidProductsByUserId(user.getId(), List.of(BoxStatus.CANCELLED, BoxStatus.RETURNED)))
                .thenReturn(List.of(op));
        when(priceService.getSystemCurrency()).thenReturn(Currency.RUB);
        when(currencyExchangeService.exchange(any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        balanceService.enrichBalance(balance);

        // Account is 100. Spendings are 40. ProjectedAccount should be 60.
        assertEquals(new BigDecimal("60.00"), balance.getProjectedAccount());
    }
}
