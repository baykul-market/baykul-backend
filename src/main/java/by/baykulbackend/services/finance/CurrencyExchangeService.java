package by.baykulbackend.services.finance;

import by.baykulbackend.database.dao.finance.Currency;
import by.baykulbackend.database.dao.finance.CurrencyExchange;
import by.baykulbackend.database.dto.finance.CurrencyDto;
import by.baykulbackend.database.dto.finance.CurrencyExchangeDto;
import by.baykulbackend.database.repository.finance.ICurrencyExchangeRepository;
import by.baykulbackend.exceptions.NotFoundException;
import by.baykulbackend.services.user.AuthService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyExchangeService {
    private static final String RUB = "RUB";
    private static final int RATE_SCALE = 6;

    private final AuthService authService;
    private final ICurrencyExchangeRepository iCurrencyExchangeRepository;

    /**
     * Creates or updates currency exchange rates.
     * Can create exchange rate in both directions if bothDirections flag is true.
     * Can replace existing rates if replaceExisting flag is true.
     *
     * @param currencyExchangeDto DTO containing exchange rate data
     * @return ResponseEntity with operation result or error message
     */
    @Transactional
    public ResponseEntity<?> createUpdateCurrencyExchange(CurrencyExchangeDto currencyExchangeDto) {
        Map<String, Object> response = new HashMap<>();

        if (isNotValidCurrencyExchange(currencyExchangeDto, response)) {
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        boolean replaceExisting =
                currencyExchangeDto.getReplaceExisting() != null && currencyExchangeDto.getReplaceExisting();

        // Check if main direction already exists
        CurrencyExchange currencyExchangeFromDb = iCurrencyExchangeRepository.findByCurrencyFromAndCurrencyTo(
                currencyExchangeDto.getCurrencyFrom(),
                currencyExchangeDto.getCurrencyTo()
        ).orElse(null);

        if (currencyExchangeFromDb != null && !replaceExisting) {
            response.put("error", "Currency exchange already exists");
            log.warn("Currency exchange already exists for {} -> {}",
                    currencyExchangeDto.getCurrencyFrom(), currencyExchangeDto.getCurrencyTo());
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }

        // Prepare main direction
        CurrencyExchange currencyExchange = new CurrencyExchange();
        currencyExchange.setCurrencyFrom(currencyExchangeDto.getCurrencyFrom());
        currencyExchange.setCurrencyTo(currencyExchangeDto.getCurrencyTo());
        currencyExchange.setRate(currencyExchangeDto.getRate());

        // Prepare reversed direction if needed
        CurrencyExchange currencyExchangeReversed = null;
        CurrencyExchange currencyExchangeReversedFromDb = null;

        if (currencyExchangeDto.getBothDirections() != null && currencyExchangeDto.getBothDirections()) {
            currencyExchangeReversedFromDb = iCurrencyExchangeRepository.findByCurrencyFromAndCurrencyTo(
                    currencyExchangeDto.getCurrencyTo(),
                    currencyExchangeDto.getCurrencyFrom()
            ).orElse(null);

            if (currencyExchangeReversedFromDb != null && !replaceExisting) {
                response.put("error", "Reversed currency exchange already exists");
                log.warn("Reversed currency exchange already exists for {} -> {}",
                        currencyExchangeDto.getCurrencyTo(), currencyExchangeDto.getCurrencyFrom());
                return new ResponseEntity<>(response, HttpStatus.CONFLICT);
            }

            currencyExchangeReversed = new CurrencyExchange();
            currencyExchangeReversed.setCurrencyFrom(currencyExchangeDto.getCurrencyTo());
            currencyExchangeReversed.setCurrencyTo(currencyExchangeDto.getCurrencyFrom());
            // Calculate reverse rate with proper scale
            currencyExchangeReversed.setRate(
                    BigDecimal.ONE.divide(currencyExchangeDto.getRate(), RATE_SCALE, RoundingMode.HALF_UP)
            );
        }

        // Save main direction (delete old if exists)
        if (currencyExchangeFromDb != null) {
            iCurrencyExchangeRepository.delete(currencyExchangeFromDb);
        }
        iCurrencyExchangeRepository.save(currencyExchange);

        // Save reversed direction if needed (delete old if exists)
        if (currencyExchangeReversed != null) {
            if (currencyExchangeReversedFromDb != null) {
                iCurrencyExchangeRepository.delete(currencyExchangeReversedFromDb);
            }
            iCurrencyExchangeRepository.save(currencyExchangeReversed);
        }

        response.put("create_currency_exchange", "true");
        log.info("Currency exchange for {} -> {} has been created/updated by user: {}",
                currencyExchangeDto.getCurrencyFrom(),
                currencyExchangeDto.getCurrencyTo(),
                authService.getAuthInfo().getPrincipal());

        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a currency exchange rate by ID
     *
     * @param id UUID of the currency exchange to delete
     * @return ResponseEntity with operation result
     * @throws NotFoundException if currency exchange not found
     */
    @Transactional
    public ResponseEntity<?> deleteById(UUID id) {
        Map<String, String> response = new HashMap<>();

        CurrencyExchange currencyExchange = iCurrencyExchangeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Currency exchange not found"));

        iCurrencyExchangeRepository.deleteById(id);
        response.put("delete_currency_exchange", "true");

        log.info("Deleted currency exchange with id = {} for {} -> {} by user: {}",
                id,
                currencyExchange.getCurrencyFrom(),
                currencyExchange.getCurrencyTo(),
                authService.getAuthInfo().getPrincipal());

        return ResponseEntity.ok(response);
    }

    /**
     * Exchanges given amount from one currency to another
     *
     * @param valueToExchange amount to exchange
     * @param currencyFrom source currency code
     * @param currencyTo target currency code
     * @return exchanged amount
     * @throws IllegalArgumentException if request is invalid
     * @throws NotFoundException if exchange rate not found
     */
    @Transactional
    public BigDecimal exchange(BigDecimal valueToExchange, Currency currencyFrom, Currency currencyTo) {
        if (!isValidExchangeRequest(valueToExchange)) {
            throw new IllegalArgumentException("Invalid exchange request");
        }

        if (currencyFrom.equals(currencyTo)) {
            return valueToExchange;
        }

        CurrencyExchange currencyExchange =
                iCurrencyExchangeRepository.findByCurrencyFromAndCurrencyTo(currencyFrom, currencyTo)
                        .orElseThrow(
                                () -> new NotFoundException(
                                        String.format(
                                                "Currency exchange not found for %s -> %s",
                                                currencyFrom.name(),
                                                currencyTo.name()
                                        )
                                )
                        );

        return valueToExchange.multiply(currencyExchange.getRate()).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Retrieves all supported currencies with their details
     *
     * @return List of CurrencyDto objects containing currency codes, Russian names, and countries of use
     */
    public List<CurrencyDto> getAllCurrencies() {
        List<CurrencyDto> currencies = new ArrayList<>();

        for (Currency currency : Currency.values()) {
            CurrencyDto dto = new CurrencyDto();
            dto.setCode(currency.name());
            dto.setRussianName(currency.getRussianName());
            dto.setCountries(currency.getCountries());
            currencies.add(dto);
        }

        return currencies;
    }

    /**
     * Validates currency exchange DTO
     *
     * @param currencyExchangeDto DTO to validate
     * @param response map to collect validation errors
     * @return true if DTO is invalid, false otherwise
     */
    private boolean isNotValidCurrencyExchange(CurrencyExchangeDto currencyExchangeDto, Map<String, Object> response) {
        if (currencyExchangeDto.getRate() == null) {
            response.put("error_rate", "Rate value must not be empty");
            log.warn("Rate value must not be empty");
            return true;
        }

        if (currencyExchangeDto.getRate().compareTo(BigDecimal.ZERO) <= 0) {
            response.put("error_rate", "Rate value must be greater than zero");
            log.warn("Rate value must be greater than zero");
            return true;
        }

        return false;
    }

    /**
     * Validates exchange request parameters
     *
     * @param valueToExchange amount to exchange
     * @return true if request is valid, false otherwise
     */
    private boolean isValidExchangeRequest(BigDecimal valueToExchange) {
        return valueToExchange != null && valueToExchange.compareTo(BigDecimal.ZERO) > 0;
    }
}