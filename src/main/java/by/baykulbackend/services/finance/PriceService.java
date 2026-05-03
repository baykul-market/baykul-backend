package by.baykulbackend.services.finance;

import by.baykulbackend.database.dao.balance.Balance;
import by.baykulbackend.database.dao.finance.PriceConfig;
import by.baykulbackend.database.dao.finance.DeliveryCostConfig;
import by.baykulbackend.database.dao.finance.DeliveryMarkupType;
import by.baykulbackend.database.dao.finance.Currency;
import by.baykulbackend.database.dao.product.Part;
import by.baykulbackend.database.dao.user.User;
import by.baykulbackend.database.dto.finance.DeliveryCostConfigDto;
import by.baykulbackend.database.dto.finance.PriceConfigDto;
import by.baykulbackend.database.repository.balance.IBalanceRepository;
import by.baykulbackend.database.repository.finance.IDeliveryCostConfigRepository;
import by.baykulbackend.database.repository.finance.IPriceConfigRepository;
import by.baykulbackend.database.repository.user.IUserRepository;
import by.baykulbackend.exceptions.NotFoundException;
import by.baykulbackend.services.user.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceService {
    private final IPriceConfigRepository iPriceConfigRepository;
    private final IDeliveryCostConfigRepository iDeliveryCostConfigRepository;
    private final IUserRepository iUserRepository;
    private final AuthService authService;
    private final CurrencyExchangeService currencyExchangeService;

    private static final BigDecimal DEFAULT_MARKUP_PERCENTAGE = new BigDecimal("0.10");
    private static final Currency DEFAULT_CURRENCY = Currency.RUB;
    private final IBalanceRepository iBalanceRepository;


    /**
     * Retrieves all global price configurations (markup, currency and global delivery rules).
     * If no configuration exists, creates a default one.
     *
     * @return PriceConfigDto containing markup percentage, system currency and delivery cost rules
     */
    @Transactional(readOnly = true)
    public PriceConfigDto getAllConfigs() {
        PriceConfigDto dto = new PriceConfigDto();

        PriceConfig config = iPriceConfigRepository.findFirst()
                .orElseGet(this::createDefaultConfig);

        dto.setMarkupPercentage(config.getMarkupPercentage());
        dto.setSystemCurrency(config.getCurrency());
        dto.setDeliveryCurrency(config.getDeliveryCurrency());
        dto.setRoundingScale(config.getRoundingScale());
        dto.setRoundingMode(config.getRoundingMode());

        // Return only global rules in the general config view
        List<DeliveryCostConfig> deliveryConfigs = iDeliveryCostConfigRepository.findAllByUserIsNullOrderByMinimumSumAsc();
        dto.setDeliveryCostConfigs(deliveryConfigs.stream()
                .map(this::mapToDeliveryDto)
                .collect(Collectors.toList()));

        return dto;
    }

    /**
     * Retrieves all delivery cost rules for a specific user.
     *
     * @param userId UUID of the target user
     * @return list of delivery cost rule DTOs belonging to that user
     * @throws NotFoundException if the user is not found
     */
    @Transactional(readOnly = true)
    public List<DeliveryCostConfigDto> getDeliveryRulesForUser(UUID userId) {
        if (!iUserRepository.existsById(userId)) {
            throw new NotFoundException("User not found: " + userId);
        }

        return iDeliveryCostConfigRepository.findAllByUserIdOrderByMinimumSumAsc(userId).stream()
                .map(this::mapToDeliveryDto)
                .collect(Collectors.toList());
    }

    /**
     * Updates markup percentage and/or system currency.
     *
     * @param configDto DTO containing fields to update (optional)
     * @return ResponseEntity with success/error message
     */
    @Transactional
    public ResponseEntity<?> updateBaseConfig(PriceConfigDto configDto) {
        Map<String, Object> response = new HashMap<>();

        if (configDto.getMarkupPercentage() != null &&
                configDto.getMarkupPercentage().compareTo(BigDecimal.ZERO) < 0) {
            response.put("error_markup", "Markup percentage must be greater than or equal to zero");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        PriceConfig config = iPriceConfigRepository.findFirst().orElse(createDefaultConfig());

        if (configDto.getMarkupPercentage() != null) {
            config.setMarkupPercentage(configDto.getMarkupPercentage());
        }

        if (configDto.getSystemCurrency() != null) {
            if (!config.getCurrency().equals(configDto.getSystemCurrency())) {
                updateBalancesCurrency(configDto.getSystemCurrency());
            }

            config.setCurrency(configDto.getSystemCurrency());
        }

        if (configDto.getDeliveryCurrency() != null) {
            config.setDeliveryCurrency(configDto.getDeliveryCurrency());
        }
        if (configDto.getRoundingScale() != null) {
            config.setRoundingScale(configDto.getRoundingScale());
        }
        if (configDto.getRoundingMode() != null) {
            config.setRoundingMode(configDto.getRoundingMode());
        }

        iPriceConfigRepository.save(config);

        response.put("update_base_config", "true");
        log.info("Base price configuration updated by user: {}",
                authService.getAuthInfo().getPrincipal());

        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new delivery cost rule.
     * If {@code dto.getUserId()} is set, the rule is created as a personal override for that user.
     * Otherwise, it is created as a global default rule.
     *
     * @param dto Delivery cost rule data
     * @return ResponseEntity with success/error message
     */
    @Transactional
    public ResponseEntity<?> createDeliveryCostRule(DeliveryCostConfigDto dto) {
        Map<String, Object> response = new HashMap<>();

        User owner = resolveOwner(dto.getUserId(), response);
        if (owner == null && dto.getUserId() != null) {
            // resolveOwner put an error into response already
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        if (isNotValidDeliveryCostConfigDto(dto, owner, response)) {
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        DeliveryCostConfig config = new DeliveryCostConfig();
        config.setMinimumSum(dto.getMinimumSum());
        config.setMarkupType(dto.getMarkupType());
        config.setValue(dto.getValue());
        config.setUser(owner);

        iDeliveryCostConfigRepository.save(config);

        response.put("save_delivery_rule", "true");
        response.put("id", config.getId().toString());
        log.info("Delivery cost rule saved by user: {}", authService.getAuthInfo().getPrincipal());

        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing delivery cost rule.
     * The rule's user association cannot be changed via this method; it is determined at creation.
     *
     * @param dto Delivery cost rule data
     * @return ResponseEntity with success/error message
     */
    @Transactional
    public ResponseEntity<?> updateDeliveryCostRule(DeliveryCostConfigDto dto) {
        Map<String, Object> response = new HashMap<>();

        if (dto.getId() == null) {
            response.put("error", "Id must not be null");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        DeliveryCostConfig config = iDeliveryCostConfigRepository.findById(dto.getId())
                .orElseThrow(() -> new NotFoundException("Delivery cost rule not found"));

        // Validate against the existing owner of the rule (immutable on update)
        User owner = config.getUser();
        if (isNotValidDeliveryCostConfigDto(dto, owner, response)) {
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        config.setMinimumSum(dto.getMinimumSum());
        config.setMarkupType(dto.getMarkupType());
        config.setValue(dto.getValue());

        iDeliveryCostConfigRepository.save(config);

        response.put("save_delivery_rule", "true");
        response.put("id", config.getId().toString());
        log.info("Delivery cost rule saved by user: {}", authService.getAuthInfo().getPrincipal());

        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a delivery cost rule by ID.
     *
     * @param id UUID of the delivery cost rule to delete
     * @return ResponseEntity with success/error message
     * @throws NotFoundException if rule not found
     */
    @Transactional
    public ResponseEntity<?> deleteDeliveryCostRule(UUID id) {
        Map<String, String> response = new HashMap<>();

        DeliveryCostConfig config = iDeliveryCostConfigRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Delivery cost rule not found"));

        iDeliveryCostConfigRepository.delete(config);

        response.put("delete_delivery_rule", "true");
        log.info("Delivery cost rule {} deleted by user: {}",
                id, authService.getAuthInfo().getPrincipal());

        return ResponseEntity.ok(response);
    }

    /**
     * Resets all configurations to default values.
     *
     * @return ResponseEntity with success message
     */
    @Transactional
    public ResponseEntity<?> resetAllToDefault() {
        if (!getSystemCurrency().equals(DEFAULT_CURRENCY)) {
            updateBalancesCurrency(DEFAULT_CURRENCY);
        }

        iPriceConfigRepository.deleteAll();
        iDeliveryCostConfigRepository.deleteAll();

        Map<String, String> response = new HashMap<>();
        response.put("reset_all_configs", "true");

        log.info("All price configurations reset to default by user: {}",
                authService.getAuthInfo().getPrincipal());

        return ResponseEntity.ok(response);
    }


    /**
     * Gets current markup percentage.
     *
     * @return current markup percentage
     */
    @Transactional(readOnly = true)
    public BigDecimal getMarkupPercentage() {
        return iPriceConfigRepository.findFirst()
                .map(PriceConfig::getMarkupPercentage)
                .orElse(DEFAULT_MARKUP_PERCENTAGE);
    }

    /**
     * Gets current system currency.
     *
     * @return current system currency
     */
    @Transactional(readOnly = true)
    public Currency getSystemCurrency() {
        return iPriceConfigRepository.findFirst()
                .map(PriceConfig::getCurrency)
                .orElse(DEFAULT_CURRENCY);
    }

    /**
     * Calculates delivery cost based on order sum using the Fallback Model.
     *
     * <p>Lookup strategy:
     * <ol>
     *   <li>If {@code user} is not null and has a UUID, look for a personal rule first.</li>
     *   <li>If no personal rule exists, fall back to the global rule (user_id IS NULL).</li>
     *   <li>If neither matches, delivery cost is zero.</li>
     * </ol>
     *
     * @param orderSum total order amount in system currency
     * @param user     the user placing the order (may be null for anonymous/fallback-only lookup)
     * @return calculated delivery cost in system currency
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateDeliveryCost(BigDecimal orderSum, User user) {
        if (orderSum == null || orderSum.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        PriceConfig priceConfig = iPriceConfigRepository.findFirst().orElse(null);
        Currency deliveryCurrency = (priceConfig != null && priceConfig.getDeliveryCurrency() != null)
                ? priceConfig.getDeliveryCurrency()
                : getSystemCurrency();

        BigDecimal orderSumInDeliveryCurrency = currencyExchangeService.exchange(
                orderSum, getSystemCurrency(), deliveryCurrency
        );

        Optional<DeliveryCostConfig> config;

        if (user != null && user.getId() != null) {
            // Fallback Model: personal rule first, global rule as fallback
            config = iDeliveryCostConfigRepository.findDeliveryCost(orderSumInDeliveryCurrency, user.getId());
        } else {
            // Anonymous / no user context: global rules only
            config = iDeliveryCostConfigRepository.findDeliveryCost(orderSumInDeliveryCurrency);
        }

        if (config.isEmpty()) {
            return BigDecimal.ZERO;
        }

        DeliveryCostConfig rule = config.get();
        BigDecimal deliveryCostInDeliveryCurrency;

        if (rule.getMarkupType() == DeliveryMarkupType.PERCENTAGE) {
            deliveryCostInDeliveryCurrency = orderSumInDeliveryCurrency.multiply(rule.getValue())
                    .setScale(2, RoundingMode.HALF_UP);
        } else {
            deliveryCostInDeliveryCurrency = rule.getValue();
        }

        return currencyExchangeService.exchange(
                deliveryCostInDeliveryCurrency, deliveryCurrency, getSystemCurrency()
        );
    }

    /**
     * Calculates delivery cost based on order sum using global rules only (no user context).
     *
     * @param orderSum total order amount in system currency
     * @return calculated delivery cost in system currency
     * @deprecated Use {@link #calculateDeliveryCost(BigDecimal, User)} to support per-user rates.
     */
    @Deprecated
    @Transactional(readOnly = true)
    public BigDecimal calculateDeliveryCost(BigDecimal orderSum) {
        return calculateDeliveryCost(orderSum, null);
    }

    /**
     * Calculates final price for a part.
     *
     * @param part         the part with price and currency
     * @param withDelivery true to include delivery cost
     * @param user         user entity to count price for (determines personal markup and delivery rate)
     * @return final price in system currency
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateProductPrice(Part part, boolean withDelivery, User user) {
        BigDecimal basePrice = currencyExchangeService.exchange(
                part.getPrice(),
                part.getCurrency(),
                getSystemCurrency()
        );

        BigDecimal userMarkupPercentage = (user != null && user.getMarkupPercentage() != null)
                ? user.getMarkupPercentage()
                : getMarkupPercentage();

        BigDecimal finalPrice = basePrice;

        if (withDelivery) {
            // Pass the user so the Fallback Model is applied for delivery as well
            BigDecimal deliveryCost = calculateDeliveryCost(basePrice, user);
            finalPrice = finalPrice.add(deliveryCost);
        }

        BigDecimal markupAmount = finalPrice.multiply(userMarkupPercentage)
                .setScale(2, RoundingMode.HALF_UP);
        finalPrice = finalPrice.add(markupAmount);

        PriceConfig config = iPriceConfigRepository.findFirst().orElse(null);
        Integer scale = (config != null && config.getRoundingScale() != null) ? config.getRoundingScale() : 2;
        RoundingMode mode = (config != null && config.getRoundingMode() != null) ? config.getRoundingMode() : RoundingMode.HALF_UP;

        return finalPrice.setScale(scale, mode);
    }

    /**
     * Calculates final price for a part.
     * Takes the currently authenticated user's params (markup + personal delivery rates).
     *
     * @param part         the part with price and currency
     * @param withDelivery true to include delivery cost
     * @return final price in system currency
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateProductPrice(Part part, boolean withDelivery) {
        return calculateProductPrice(
                part,
                withDelivery,
                iUserRepository.findByLogin(authService.getAuthInfo().getPrincipal().toString()).orElse(null)
        );
    }


    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Creates default price configuration.
     *
     * @return saved PriceConfig entity with default values
     */
    private PriceConfig createDefaultConfig() {
        PriceConfig defaultConfig = new PriceConfig();
        defaultConfig.setMarkupPercentage(DEFAULT_MARKUP_PERCENTAGE);
        defaultConfig.setCurrency(DEFAULT_CURRENCY);

        return defaultConfig;
    }

    /**
     * Maps DeliveryCostConfig entity to DTO.
     *
     * @param config the entity to map
     * @return DeliveryCostConfigDto
     */
    private DeliveryCostConfigDto mapToDeliveryDto(DeliveryCostConfig config) {
        DeliveryCostConfigDto dto = new DeliveryCostConfigDto();
        dto.setId(config.getId());
        dto.setMinimumSum(config.getMinimumSum());
        dto.setMarkupType(config.getMarkupType());
        dto.setValue(config.getValue());
        dto.setUserId(config.getUser() != null ? config.getUser().getId() : null);

        return dto;
    }

    /**
     * Resolves an optional owner User from a userId.
     * Returns null (no owner) when userId is null — meaning a global rule.
     * If userId is non-null but user is not found, puts an error into the response map and returns null.
     *
     * @param userId   optional user UUID (null → global rule)
     * @param response mutable response map to put errors into
     * @return resolved User, or null if userId was null
     */
    private User resolveOwner(UUID userId, Map<String, Object> response) {
        if (userId == null) {
            return null;
        }

        Optional<User> user = iUserRepository.findById(userId);
        if (user.isEmpty()) {
            response.put("error", "User not found: " + userId);
            return null;
        }

        return user.get();
    }

    /**
     * Validates a DeliveryCostConfigDto, taking into account its owner (user).
     * Duplicate-check is user-aware: global rules are checked against global rules only,
     * and user-specific rules are checked within that user's scope.
     *
     * @param dto      the DTO to validate
     * @param owner    the resolved User owner (null = global rule)
     * @param response mutable response map to put errors into
     * @return true if validation failed (rule is NOT valid)
     */
    private boolean isNotValidDeliveryCostConfigDto(DeliveryCostConfigDto dto, User owner, Map<String, Object> response) {
        if (dto.getMinimumSum() == null || dto.getMinimumSum().compareTo(BigDecimal.ZERO) < 0) {
            response.put("error", "Minimum sum must be greater than or equal to zero");
            return true;
        }

        if (dto.getMarkupType() == null) {
            response.put("error", "Markup type must not be empty");
            return true;
        }

        if (dto.getValue() == null || dto.getValue().compareTo(BigDecimal.ZERO) < 0) {
            response.put("error", "Value must be greater than or equal to zero");
            return true;
        }

        if (owner == null) {
            // Global rule duplicate check
            if (iDeliveryCostConfigRepository.existsByMinimumSumAndUserIsNullAndIdNotIn(
                    dto.getMinimumSum(), Collections.singletonList(dto.getId())
            )) {
                response.put("error", "Global delivery cost rule with this minimum sum already exists");
                return true;
            }
        } else {
            // User-specific rule duplicate check
            if (iDeliveryCostConfigRepository.existsByMinimumSumAndUserIdAndIdNotIn(
                    dto.getMinimumSum(), owner.getId(), Collections.singletonList(dto.getId())
            )) {
                response.put("error", "Delivery cost rule with this minimum sum already exists for this user");
                return true;
            }
        }

        return false;
    }

    private void updateBalancesCurrency(Currency newCurrency) {
        Set<Balance> balances = iBalanceRepository.findAllByCurrencyNot(newCurrency);

        for (Balance balance : balances) {
            if (balance.getCurrency().equals(newCurrency)) {
                continue;
            }

            balance.setAccount(currencyExchangeService.exchange(
                    balance.getAccount(), balance.getCurrency(), newCurrency
            ));
            balance.setCurrency(newCurrency);
        }

        iBalanceRepository.saveAll(balances);
    }
}