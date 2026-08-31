package ru.bankapi.service.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bankapi.dal.BankAccountRepository;
import ru.bankapi.dal.CardRepository;
import ru.bankapi.dto.account.AccountResponse;
import ru.bankapi.enums.AccountStatus;
import ru.bankapi.enums.CardStatus;
import ru.bankapi.exception.InvalidOperationException;
import ru.bankapi.exception.NotFoundException;
import ru.bankapi.mapper.BankAccountMapper;
import ru.bankapi.model.BankAccount;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final CardRepository cardRepository;
    private final BankAccountMapper bankAccountMapper;

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccounts() {
        return bankAccountRepository.findAll()
                .stream()
                .map(bankAccountMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(Long accountId) {
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Счёт не найден"));

        return bankAccountMapper.toResponse(account);
    }

    @Transactional
    public AccountResponse blockAccount(Long accountId) {
        BankAccount account = getAccountForUpdate(accountId);

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new InvalidOperationException("Закрытый счёт нельзя заблокировать");
        }

        if (account.getStatus() == AccountStatus.BLOCKED) {
            throw new InvalidOperationException("Счёт уже заблокирован");
        }

        account.setStatus(AccountStatus.BLOCKED);

        return bankAccountMapper.toResponse(account);
    }

    @Transactional
    public AccountResponse unblockAccount(Long accountId) {
        BankAccount account = getAccountForUpdate(accountId);

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new InvalidOperationException("Закрытый счёт нельзя разблокировать");
        }

        if (account.getStatus() == AccountStatus.ACTIVE) {
            throw new InvalidOperationException("Счёт уже активен");
        }

        account.setStatus(AccountStatus.ACTIVE);

        return bankAccountMapper.toResponse(account);
    }

    @Transactional
    public AccountResponse closeAccount(Long accountId) {
        BankAccount account = getAccountForUpdate(accountId);

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new InvalidOperationException("Счёт уже закрыт");
        }

        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new InvalidOperationException("Нельзя закрыть счёт с ненулевым балансом");
        }

        account.setStatus(AccountStatus.CLOSED);

        cardRepository.findByBankAccountId(accountId).ifPresent(card -> card.setStatus(CardStatus.CLOSED));

        return bankAccountMapper.toResponse(account);
    }

    private BankAccount getAccountForUpdate(Long accountId) {
        return bankAccountRepository
                .findByIdForUpdate(accountId)
                .orElseThrow(() -> new NotFoundException("Счёт не найден"));
    }
}