package by.baykulbackend.database.repository.finance;

import by.baykulbackend.database.dao.finance.CurrencyExchange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ICurrencyExchangeRepository extends JpaRepository<CurrencyExchange, UUID> {
    Optional<CurrencyExchange> findByCurrencyFromAndCurrencyTo(String currencyTo, String currencyFrom);
}
