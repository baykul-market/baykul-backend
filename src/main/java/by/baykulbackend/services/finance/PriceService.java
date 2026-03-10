package by.baykulbackend.services.finance;

import by.baykulbackend.database.dao.finance.*;
import by.baykulbackend.database.dao.finance.Currency;
import by.baykulbackend.database.dao.product.Part;
import by.baykulbackend.database.dao.user.User;
import by.baykulbackend.database.dto.finance.DeliveryCostConfigDto;
import by.baykulbackend.database.dto.finance.PriceConfigDto;
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
import java.util.*;
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


    /**
     * Retrieves all price configurations (markup, currency and delivery rules).
     * If no configuration exists, creates default one.
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

        List<DeliveryCostConfig> deliveryConfigs = iDeliveryCostConfigRepository.findAllByOrderByMinimumSumAsc();
        dto.setDeliveryCostConfigs(deliveryConfigs.stream()
                .map(this::mapToDeliveryDto)
                .collect(Collectors.toList()));

        return dto;
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
            config.setCurrency(configDto.getSystemCurrency());
        }

        iPriceConfigRepository.save(config);

        response.put("update_base_config", "true");
        log.info("Base price configuration updated by user: {}",
                authService.getAuthInfo().getPrincipal());

        return ResponseEntity.ok(response);
    }

    /**
     * Creates or updates delivery cost rule.
     * For SUM type, currency is required and defaults to system currency if not provided.
     *
     * @param dto Delivery cost rule data
     * @return ResponseEntity with success/error message
     */
    @Transactional
    public ResponseEntity<?> saveDeliveryCostRule(DeliveryCostConfigDto dto) {
        Map<String, Object> response = new HashMap<>();

        if (dto.getMinimumSum() == null || dto.getMinimumSum().compareTo(BigDecimal.ZERO) < 0) {
            response.put("error", "Minimum sum must be greater than or equal to zero");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if (dto.getMarkupType() == null) {
            response.put("error", "Markup type must not be empty");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if (dto.getValue() == null || dto.getValue().compareTo(BigDecimal.ZERO) < 0) {
            response.put("error", "Value must be greater than or equal to zero");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        DeliveryCostConfig config;
        if (dto.getId() != null) {
            config = iDeliveryCostConfigRepository.findById(dto.getId())
                    .orElseThrow(() -> new NotFoundException("Delivery cost rule not found"));
        } else {
            config = new DeliveryCostConfig();
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
     * Calculates delivery cost based on order sum.
     *
     * @param orderSum total order amount
     * @return calculated delivery cost
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateDeliveryCost(BigDecimal orderSum) {
        if (orderSum == null || orderSum.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        Optional<DeliveryCostConfig> config = iDeliveryCostConfigRepository.findDeliveryCost(orderSum);

        if (config.isEmpty()) {
            return BigDecimal.ZERO;
        }

        DeliveryCostConfig rule = config.get();

        if (rule.getMarkupType() == DeliveryMarkupType.PERCENTAGE) {
            return orderSum.multiply(rule.getValue()).setScale(2, RoundingMode.HALF_UP);
        } else {
            return rule.getValue();
        }
    }

    /**
     * Calculates final price for a part.
     *
     * @param part the part with price and currency
     * @param withDelivery true to include delivery cost
     * @return final price in system currency
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateProductPrice(Part part, boolean withDelivery) {
        BigDecimal basePrice = currencyExchangeService.exchange(
                part.getPrice(),
                part.getCurrency(),
                getSystemCurrency()
        );

        User user = iUserRepository.findByLogin(authService.getAuthInfo().getPrincipal().toString()).orElse(null);
        BigDecimal userMarkupPercentage = user != null ? user.getMarkupPercentage() : getMarkupPercentage();

        BigDecimal finalPrice = basePrice;

        if (withDelivery) {
            BigDecimal deliveryCost = calculateDeliveryCost(basePrice);
            finalPrice = finalPrice.add(deliveryCost);
        }

        BigDecimal markupAmount = finalPrice.multiply(userMarkupPercentage)
                .setScale(2, RoundingMode.HALF_UP);
        finalPrice = finalPrice.add(markupAmount).setScale(2, RoundingMode.HALF_UP);

        return finalPrice;
    }


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

        return dto;
    }
}