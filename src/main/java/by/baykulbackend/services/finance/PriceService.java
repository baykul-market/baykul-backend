package by.baykulbackend.services.finance;

import by.baykulbackend.database.dao.finance.Currency;
import by.baykulbackend.database.dao.finance.PriceConfig;
import by.baykulbackend.database.dao.product.Part;
import by.baykulbackend.database.dao.user.User;
import by.baykulbackend.database.dto.finance.PriceConfigDto;
import by.baykulbackend.database.repository.finance.IPriceConfigRepository;
import by.baykulbackend.database.repository.user.IUserRepository;
import by.baykulbackend.services.user.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceService {
    private final IPriceConfigRepository iPriceConfigRepository;
    private final IUserRepository iUserRepository;
    private final AuthService authService;

    private static final BigDecimal DEFAULT_DELIVERY_PERCENTAGE = new BigDecimal("0.10");
    private static final BigDecimal DEFAULT_MARKUP_PERCENTAGE = new BigDecimal("0.10");
    private static final Currency DEFAULT_CURRENCY = Currency.RUB;
    private final CurrencyExchangeService currencyExchangeService;

    /**
     * Retrieves the current price configuration.
     * If no configuration exists, creates one with default values (10% delivery, 10% markup, RUB currency).
     *
     * @return PriceConfigDto containing delivery percentage, markup percentage and currency
     */
    @Transactional(readOnly = true)
    public PriceConfigDto getConfig() {
        PriceConfig config = iPriceConfigRepository.findFirst()
                .orElseGet(this::createDefaultConfig);

        return mapToDto(config);
    }

    /**
     * Updates price configuration with partial or full data.
     * If configuration doesn't exist, creates new one.
     * For missing fields:
     * - If config exists: keeps existing values
     * - If config doesn't exist: uses default values (10% delivery, 10% markup, RUB)
     *
     * @param configDto DTO containing fields to update (can be partial)
     * @return ResponseEntity with success message or validation errors
     */
    @Transactional
    public ResponseEntity<?> updateConfig(PriceConfigDto configDto) {
        Map<String, Object> response = new HashMap<>();

        if (isNotValidPartialConfig(configDto, response)) {
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        PriceConfig config = iPriceConfigRepository.findFirst()
                .orElse(new PriceConfig());

        boolean isNewConfig = config.getId() == null;

        if (configDto.getDeliveryPercentage() != null) {
            config.setDeliveryPercentage(configDto.getDeliveryPercentage());
        } else if (isNewConfig) {
            config.setDeliveryPercentage(DEFAULT_DELIVERY_PERCENTAGE);
        }

        if (configDto.getMarkupPercentage() != null) {
            config.setMarkupPercentage(configDto.getMarkupPercentage());
        } else if (isNewConfig) {
            config.setMarkupPercentage(DEFAULT_MARKUP_PERCENTAGE);
        }

        if (configDto.getCurrency() != null) {
            config.setCurrency(configDto.getCurrency());
        } else if (isNewConfig) {
            config.setCurrency(DEFAULT_CURRENCY);
        }

        iPriceConfigRepository.save(config);
        response.put("update_price_config", "true");
        log.info("Price configuration updated by user: {}", authService.getAuthInfo().getPrincipal());

        return ResponseEntity.ok(response);
    }

    /**
     * Resets price configuration to default values (10% delivery, 10% markup, RUB currency).
     * Requires admin privileges.
     *
     * @return ResponseEntity with success message
     */
    @Transactional
    public ResponseEntity<?> resetToDefault() {
        PriceConfig config = iPriceConfigRepository.findFirst()
                .orElse(new PriceConfig());

        config.setDeliveryPercentage(DEFAULT_DELIVERY_PERCENTAGE);
        config.setMarkupPercentage(DEFAULT_MARKUP_PERCENTAGE);
        config.setCurrency(DEFAULT_CURRENCY);

        iPriceConfigRepository.save(config);

        Map<String, String> response = new HashMap<>();
        response.put("reset_price_config", "true");

        log.info("Price configuration reset to default by user: {}",
                authService.getAuthInfo().getPrincipal());

        return ResponseEntity.ok(response);
    }

    /**
     * Gets the current delivery percentage for calculations.
     * If no configuration exists, returns default value (10%).
     *
     * @return current delivery percentage as BigDecimal
     */
    @Transactional(readOnly = true)
    public BigDecimal getDeliveryPercentage() {
        return getConfig().getDeliveryPercentage();
    }

    /**
     * Gets the current markup percentage for calculations.
     * If no configuration exists, returns default value (10%).
     *
     * @return current markup percentage as BigDecimal
     */
    @Transactional(readOnly = true)
    public BigDecimal getMarkupPercentage() {
        return getConfig().getMarkupPercentage();
    }

    /**
     * Gets the current currency for calculations.
     * If no configuration exists, returns default value (RUB).
     *
     * @return current currency
     */
    @Transactional(readOnly = true)
    public Currency getCurrency() {
        return getConfig().getCurrency();
    }

    /**
     * Calculates the final price for a part.
     * <p>
     * Formula:
     * Final price = Exchange rate × (Base price + Delivery + Markup)
     * <p>
     * Where:
     * - Delivery = Base price × Delivery% (if withDelivery = true, else 0)
     * - Markup = (Base price + Delivery) × Markup%
     * - Exchange rate: from part currency to system currency
     * <p>
     * Example with delivery and markup:
     * Base price = 100 USD, Delivery% = 10%, Markup% = 20%, Rate = 90.5 RUB/USD
     * Delivery = 100 × 0.1 = 10 USD
     * Markup = (100 + 10) × 0.2 = 22 USD
     * Final = (100 + 10 + 22) × 90.5 = 11,946 RUB
     *
     * @param part the part with price and currency
     * @param withDelivery true to include delivery cost
     * @return final price in system currency
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateProductPrice(Part part, boolean withDelivery) {
        BigDecimal basePrice = part.getPrice();
        Currency currency = part.getCurrency();

        User user = iUserRepository.findByLogin(authService.getAuthInfo().getPrincipal().toString()).orElse(null);
        BigDecimal userMarkupPercentage = user != null ? user.getMarkupPercentage() : getMarkupPercentage();

        BigDecimal finalPrice;

        BigDecimal deliveryAmount = basePrice.multiply(withDelivery ? getDeliveryPercentage() : BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        finalPrice = basePrice.add(deliveryAmount);

        BigDecimal markupAmount = finalPrice.multiply(userMarkupPercentage)
                .setScale(2, RoundingMode.HALF_UP);

        finalPrice = finalPrice.add(markupAmount).setScale(2, RoundingMode.HALF_UP);

        finalPrice = currencyExchangeService.exchange(finalPrice, currency, getCurrency());

        log.debug("Calculated final price: base={}, delivery={}, markup={}, final={}",
                basePrice, deliveryAmount, markupAmount, finalPrice);

        return finalPrice;
    }

    /**
     * Creates default configuration with preset values.
     * Used when no configuration exists in the database.
     *
     * @return saved PriceConfig entity with default values
     */
    private PriceConfig createDefaultConfig() {
        PriceConfig defaultConfig = new PriceConfig();
        defaultConfig.setDeliveryPercentage(DEFAULT_DELIVERY_PERCENTAGE);
        defaultConfig.setMarkupPercentage(DEFAULT_MARKUP_PERCENTAGE);
        defaultConfig.setCurrency(DEFAULT_CURRENCY);
        return iPriceConfigRepository.save(defaultConfig);
    }

    /**
     * Maps PriceConfig entity to PriceConfigDto.
     *
     * @param config the entity to map
     * @return DTO containing delivery percentage, markup percentage and currency
     */
    private PriceConfigDto mapToDto(PriceConfig config) {
        PriceConfigDto dto = new PriceConfigDto();
        dto.setDeliveryPercentage(config.getDeliveryPercentage());
        dto.setMarkupPercentage(config.getMarkupPercentage());
        dto.setCurrency(config.getCurrency());
        return dto;
    }

    /**
     * Validates the configuration DTO for partial updates.
     * Checks that provided fields are valid.
     *
     * @param configDto DTO to validate
     * @param response map to collect validation errors
     * @return true if validation fails, false if validation passes
     */
    private boolean isNotValidPartialConfig(PriceConfigDto configDto, Map<String, Object> response) {
        boolean hasError = false;

        if (configDto.getDeliveryPercentage() != null) {
            if (configDto.getDeliveryPercentage().compareTo(BigDecimal.ZERO) < 0) {
                response.put("error_delivery_percentage", "Delivery percentage must be greater than or equal to zero");
                log.warn("Delivery percentage must be greater than or equal to zero");
                hasError = true;
            }
        }

        if (configDto.getMarkupPercentage() != null) {
            if (configDto.getMarkupPercentage().compareTo(BigDecimal.ZERO) < 0) {
                response.put("error_markup_percentage", "Markup percentage must be greater than or equal to zero");
                log.warn("Markup percentage must be greater than or equal to zero");
                hasError = true;
            }
        }

        return hasError;
    }
}