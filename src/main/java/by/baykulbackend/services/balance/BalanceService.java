package by.baykulbackend.services.balance;

import by.baykulbackend.database.dao.balance.Balance;
import by.baykulbackend.database.dao.balance.BalanceHistory;
import by.baykulbackend.database.dto.balance.BalanceOperationDto;
import by.baykulbackend.database.repository.balance.IBalanceRepository;
import by.baykulbackend.exceptions.BadRequestException;
import by.baykulbackend.exceptions.NotFoundException;
import by.baykulbackend.services.user.AuthService;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceService {
    private final IBalanceRepository iBalanceRepository;
    private final AuthService authService;

    /**
     * Processes a balance operation.
     * Guarantees atomicity through @Transactional and pessimistic locking.
     *
     * @param balanceOperation DTO containing operation data
     * @throws BadRequestException if the operation data is invalid
     * @throws NotFoundException if the balance is not found
     */
    @Transactional
    public void processBalance(@NonNull BalanceOperationDto balanceOperation) {
        log.info("Processing balance operation {}, amount {}, balanceId {}, userId {} -> {}",
                balanceOperation.getOperationType(), balanceOperation.getAmount(), balanceOperation.getBalanceId(),
                balanceOperation.getUserId(), authService.getAuthInfo().getPrincipal().toString());

        if (balanceOperation.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Operation amount must be greater than zero");
        }

        if (balanceOperation.getAmount().scale() > 2) {
            throw new BadRequestException("Amount scale cannot exceed 2 decimal places");
        }

        if (StringUtils.isBlank(balanceOperation.getBalanceId()) && StringUtils.isBlank(balanceOperation.getUserId())) {
            throw new BadRequestException("One of the following must be filled in: balance id, user id");
        }

        Balance balance = null;

        if (StringUtils.isNotBlank(balanceOperation.getBalanceId())) {
            try {
                UUID balanceId = UUID.fromString(balanceOperation.getBalanceId());
                balance = iBalanceRepository.findByIdWithLock(balanceId).orElse(null);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid balance id while processing balance {} -> {}", balanceOperation.getBalanceId(),
                        authService.getAuthInfo().getPrincipal().toString());
                throw new BadRequestException("Invalid balance id");
            }
        }

        if (balance == null) {
            try {
                UUID userId = UUID.fromString(balanceOperation.getUserId());
                balance = iBalanceRepository.findByUserIdWithLock(userId)
                        .orElseThrow(() -> new NotFoundException("User balance not found"));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid user id while processing balance {} -> {}", balanceOperation.getUserId(),
                        authService.getAuthInfo().getPrincipal().toString());
                throw new BadRequestException("Invalid user id");
            }
        }

        BigDecimal currentAccount = balance.getAccount();
        BigDecimal newAccount;

        switch (balanceOperation.getOperationType()) {
            case REPLENISHMENT -> newAccount = currentAccount.add(balanceOperation.getAmount());
            case PAYMENT, WITHDRAWAL -> {
                if (currentAccount.compareTo(balanceOperation.getAmount()) < 0) {
                    throw new BadRequestException("Insufficient funds");
                }
                newAccount = currentAccount.subtract(balanceOperation.getAmount());
            }
            default -> {
                log.warn("Invalid balance operation while processing balance {} -> {}",
                        balanceOperation.getOperationType(), authService.getAuthInfo().getPrincipal().toString());
                throw new BadRequestException("Unexpected operation: " + balanceOperation.getOperationType());
            }
        }

        BalanceHistory balanceHistory = new BalanceHistory();
        balanceHistory.setBalance(balance);
        balanceHistory.setAmount(balanceOperation.getAmount());
        balanceHistory.setResultAccount(newAccount);
        balanceHistory.setOperationType(balanceOperation.getOperationType());
        balanceHistory.setDescription(balanceOperation.getDescription());

        balance.setAccount(newAccount);
        balance.getBalanceHistoryList().add(balanceHistory);
        iBalanceRepository.save(balance);
        
        log.info("Successfully processed balance operation {}, amount {}, balanceId {}, userId {} -> {}",
                balanceOperation.getOperationType(), balanceOperation.getAmount(), balanceOperation.getBalanceId(),
                balanceOperation.getUserId(), authService.getAuthInfo().getPrincipal().toString());
    }
}
