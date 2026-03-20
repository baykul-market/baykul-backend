package by.baykulbackend.database.repository.balance;

import by.baykulbackend.database.dao.balance.Balance;
import by.baykulbackend.database.dao.finance.Currency;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface IBalanceRepository extends JpaRepository<Balance, UUID> {
    Optional<Balance> findByUserId(UUID userId);
    Optional<Balance> findByUserLogin(String userLogin);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Balance b WHERE b.user.id = :userId")
    Optional<Balance> findByUserIdWithLock(@Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Balance b WHERE b.id = :id")
    Optional<Balance> findByIdWithLock(@Param("id") UUID id);

    Set<Balance> findAllByCurrencyNot(Currency currency);
}
