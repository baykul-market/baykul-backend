package by.baykulbackend.services.finance;

import by.baykulbackend.database.dao.finance.Currency;
import by.baykulbackend.database.dao.finance.DeliveryCostConfig;
import by.baykulbackend.database.dao.finance.DeliveryMarkupType;
import by.baykulbackend.database.dao.finance.PriceConfig;
import by.baykulbackend.database.dao.user.User;
import by.baykulbackend.database.repository.finance.IDeliveryCostConfigRepository;
import by.baykulbackend.database.repository.finance.IPriceConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PriceService#calculateDeliveryCost(BigDecimal, User)}.
 *
 * <p>Test scenarios:
 * <ul>
 *   <li>Global Fallback — user has no personal rules, global rule applies.</li>
 *   <li>User Rules — user has personal rules, they override global rules.</li>
 *   <li>Edge cases — null/zero order sum, no rules at all.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class PriceServiceDeliveryCostTest {

    @Mock
    private IDeliveryCostConfigRepository deliveryRepo;

    @Mock
    private IPriceConfigRepository priceConfigRepo;

    @Mock
    private CurrencyExchangeService currencyExchangeService;

    // The remaining dependencies are not involved in delivery cost calculation
    @Mock
    private by.baykulbackend.database.repository.user.IUserRepository userRepository;
    @Mock
    private by.baykulbackend.services.user.AuthService authService;
    @Mock
    private by.baykulbackend.database.repository.balance.IBalanceRepository balanceRepository;

    @InjectMocks
    private PriceService priceService;

    // ── Fixtures ────────────────────────────────────────────────────────────

    private static final BigDecimal ORDER_SUM       = new BigDecimal("200.00");
    private static final BigDecimal DELIVERY_COST   = new BigDecimal("15.00");
    private static final UUID       USER_ID         = UUID.randomUUID();

    private PriceConfig priceConfig;
    private User        testUser;

    @BeforeEach
    void setUpCommon() {
        priceConfig = new PriceConfig();
        priceConfig.setMarkupPercentage(new BigDecimal("0.10"));
        priceConfig.setCurrency(Currency.RUB);
        // No separate delivery currency → delivery currency == system currency
        priceConfig.setDeliveryCurrency(null);
        priceConfig.setRoundingScale(2);
        priceConfig.setRoundingMode(RoundingMode.HALF_UP);

        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setLogin("testUser");

        // System currency is always RUB in these tests
        lenient().when(priceConfigRepo.findFirst()).thenReturn(Optional.of(priceConfig));

        // Exchange with same currency → identity
        lenient().when(currencyExchangeService.exchange(any(), eq(Currency.RUB), eq(Currency.RUB)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ── Helper factories ────────────────────────────────────────────────────

    private DeliveryCostConfig fixedRule(BigDecimal minimumSum, BigDecimal cost, User owner) {
        DeliveryCostConfig rule = new DeliveryCostConfig();
        rule.setId(UUID.randomUUID());
        rule.setMinimumSum(minimumSum);
        rule.setMarkupType(DeliveryMarkupType.SUM);
        rule.setValue(cost);
        rule.setUser(owner);
        return rule;
    }

    private DeliveryCostConfig percentageRule(BigDecimal minimumSum, BigDecimal pct, User owner) {
        DeliveryCostConfig rule = new DeliveryCostConfig();
        rule.setId(UUID.randomUUID());
        rule.setMinimumSum(minimumSum);
        rule.setMarkupType(DeliveryMarkupType.PERCENTAGE);
        rule.setValue(pct);
        rule.setUser(owner);
        return rule;
    }

    // ════════════════════════════════════════════════════════════════════════
    // Scenario 1 — Global Fallback (user has no personal rules)
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Global Fallback — no personal rules for user")
    class GlobalFallbackTests {

        @Test
        @DisplayName("Should apply global rule when user has no personal rules")
        void shouldApplyGlobalRule_whenUserHasNoPersonalRules() {
            // The fallback query returns a global rule (user == null)
            DeliveryCostConfig globalRule = fixedRule(BigDecimal.ZERO, DELIVERY_COST, null);
            when(deliveryRepo.findDeliveryCost(ORDER_SUM, USER_ID)).thenReturn(Optional.of(globalRule));

            BigDecimal result = priceService.calculateDeliveryCost(ORDER_SUM, testUser);

            assertThat(result).isEqualByComparingTo(DELIVERY_COST);
            // The two-arg (fallback-aware) query must be used
            verify(deliveryRepo).findDeliveryCost(ORDER_SUM, USER_ID);
            verifyNoMoreInteractions(deliveryRepo);
        }

        @Test
        @DisplayName("Should return zero when no global rule applies either")
        void shouldReturnZero_whenNoRuleApplies() {
            when(deliveryRepo.findDeliveryCost(ORDER_SUM, USER_ID)).thenReturn(Optional.empty());

            BigDecimal result = priceService.calculateDeliveryCost(ORDER_SUM, testUser);

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should use global-only query when user is null")
        void shouldUseGlobalOnlyQuery_whenUserIsNull() {
            DeliveryCostConfig globalRule = fixedRule(BigDecimal.ZERO, DELIVERY_COST, null);
            when(deliveryRepo.findDeliveryCost(ORDER_SUM)).thenReturn(Optional.of(globalRule));

            BigDecimal result = priceService.calculateDeliveryCost(ORDER_SUM, null);

            assertThat(result).isEqualByComparingTo(DELIVERY_COST);
            // Must use the single-arg (global-only) query
            verify(deliveryRepo).findDeliveryCost(ORDER_SUM);
            verify(deliveryRepo, never()).findDeliveryCost(any(BigDecimal.class), any(UUID.class));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Scenario 2 — User has personal rules (override)
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("User-specific rules — personal tariff overrides global")
    class UserRuleTests {

        @Test
        @DisplayName("Should apply user's personal SUM rule instead of global rule")
        void shouldApplyPersonalRule_sumType() {
            BigDecimal personalCost = new BigDecimal("5.00");
            DeliveryCostConfig personalRule = fixedRule(BigDecimal.ZERO, personalCost, testUser);
            when(deliveryRepo.findDeliveryCost(ORDER_SUM, USER_ID)).thenReturn(Optional.of(personalRule));

            BigDecimal result = priceService.calculateDeliveryCost(ORDER_SUM, testUser);

            assertThat(result).isEqualByComparingTo(personalCost);
        }

        @Test
        @DisplayName("Should apply user's personal PERCENTAGE rule correctly")
        void shouldApplyPersonalRule_percentageType() {
            // 10% of 200.00 = 20.00
            BigDecimal pct = new BigDecimal("0.10");
            BigDecimal expected = ORDER_SUM.multiply(pct).setScale(2, RoundingMode.HALF_UP); // 20.00

            DeliveryCostConfig personalRule = percentageRule(BigDecimal.ZERO, pct, testUser);
            when(deliveryRepo.findDeliveryCost(ORDER_SUM, USER_ID)).thenReturn(Optional.of(personalRule));

            BigDecimal result = priceService.calculateDeliveryCost(ORDER_SUM, testUser);

            assertThat(result).isEqualByComparingTo(expected);
        }

        @Test
        @DisplayName("Repository fallback query is always used when user is provided")
        void shouldAlwaysUseFallbackQuery_whenUserProvided() {
            when(deliveryRepo.findDeliveryCost(ORDER_SUM, USER_ID)).thenReturn(Optional.empty());

            priceService.calculateDeliveryCost(ORDER_SUM, testUser);

            verify(deliveryRepo).findDeliveryCost(ORDER_SUM, USER_ID);
            verify(deliveryRepo, never()).findDeliveryCost(ORDER_SUM);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Scenario 3 — Edge cases
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should return zero for null order sum")
        void shouldReturnZero_forNullOrderSum() {
            BigDecimal result = priceService.calculateDeliveryCost(null, testUser);

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
            verifyNoInteractions(deliveryRepo);
        }

        @Test
        @DisplayName("Should return zero for zero order sum")
        void shouldReturnZero_forZeroOrderSum() {
            BigDecimal result = priceService.calculateDeliveryCost(BigDecimal.ZERO, testUser);

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
            verifyNoInteractions(deliveryRepo);
        }

        @Test
        @DisplayName("Should return zero for negative order sum")
        void shouldReturnZero_forNegativeOrderSum() {
            BigDecimal result = priceService.calculateDeliveryCost(new BigDecimal("-1.00"), testUser);

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
            verifyNoInteractions(deliveryRepo);
        }

        @Test
        @DisplayName("Should return zero when user has no ID (anonymous-like user object)")
        void shouldUseGlobalOnlyQuery_whenUserHasNoId() {
            User anonymousLikeUser = new User(); // no ID set
            DeliveryCostConfig globalRule = fixedRule(BigDecimal.ZERO, DELIVERY_COST, null);
            when(deliveryRepo.findDeliveryCost(ORDER_SUM)).thenReturn(Optional.of(globalRule));

            BigDecimal result = priceService.calculateDeliveryCost(ORDER_SUM, anonymousLikeUser);

            assertThat(result).isEqualByComparingTo(DELIVERY_COST);
            // User has no UUID → must fall back to global-only lookup
            verify(deliveryRepo).findDeliveryCost(ORDER_SUM);
            verify(deliveryRepo, never()).findDeliveryCost(any(BigDecimal.class), any(UUID.class));
        }

        @Test
        @DisplayName("Should return zero when rules list is completely empty")
        void shouldReturnZero_whenNoRulesExistAtAll() {
            when(deliveryRepo.findDeliveryCost(ORDER_SUM, USER_ID)).thenReturn(Optional.empty());

            BigDecimal result = priceService.calculateDeliveryCost(ORDER_SUM, testUser);

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should convert amounts using delivery currency when configured")
        void shouldConvertToDeliveryCurrency_whenConfigured() {
            priceConfig.setDeliveryCurrency(Currency.EUR);
            BigDecimal orderSumInEur = new BigDecimal("50.00");
            BigDecimal deliveryCostInEur = new BigDecimal("5.00");
            BigDecimal deliveryCostInRub = new BigDecimal("500.00");

            // RUB → EUR conversion
            when(currencyExchangeService.exchange(ORDER_SUM, Currency.RUB, Currency.EUR))
                    .thenReturn(orderSumInEur);
            // EUR → RUB conversion for delivery cost
            when(currencyExchangeService.exchange(deliveryCostInEur, Currency.EUR, Currency.RUB))
                    .thenReturn(deliveryCostInRub);

            DeliveryCostConfig rule = fixedRule(BigDecimal.ZERO, deliveryCostInEur, null);
            when(deliveryRepo.findDeliveryCost(orderSumInEur, USER_ID)).thenReturn(Optional.of(rule));

            BigDecimal result = priceService.calculateDeliveryCost(ORDER_SUM, testUser);

            assertThat(result).isEqualByComparingTo(deliveryCostInRub);
        }
    }
}
