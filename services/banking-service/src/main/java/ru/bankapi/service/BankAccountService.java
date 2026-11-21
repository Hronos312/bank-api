package ru.bankapi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import ru.bankapi.dal.BankAccountRepository;
import ru.bankapi.dal.UniqueConstraints;
import ru.bankapi.dto.account.AccountResponse;
import ru.bankapi.enums.AccountStatus;
import ru.bankapi.enums.CurrencyCode;
import ru.bankapi.exception.NotFoundException;
import ru.bankapi.exception.DuplicateDataException;
import ru.bankapi.generator.AccountNumberGenerator;
import ru.bankapi.mapper.BankAccountMapper;
import ru.bankapi.model.BankAccount;
import ru.bankapi.identity.IdentityDirectory;
import ru.bankapi.identity.IdentityDirectory.UserIdentity;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BankAccountService {

    private static final int MAX_NUMBER_ATTEMPTS = 5;

    private final BankAccountRepository bankAccountRepository;
    private final IdentityDirectory identityDirectory;
    private final AccountNumberGenerator accountNumberGenerator;
    private final BankAccountMapper bankAccountMapper;

    public AccountResponse createAccount(String subject) {
        UserIdentity user = getUserBySubject(subject);

        for (int attempt = 0; attempt < MAX_NUMBER_ATTEMPTS; attempt++) {
            BankAccount account = new BankAccount();
            account.setAccountNumber(accountNumberGenerator.generate());
            account.setUserId(user.id());
            account.setBalance(BigDecimal.ZERO);
            account.setCurrency(CurrencyCode.RUB);
            account.setStatus(AccountStatus.ACTIVE);

            BankAccount savedAccount;
            try {
                savedAccount = bankAccountRepository.saveAndFlush(account);
            } catch (DataIntegrityViolationException exception) {
                if (!UniqueConstraints.matches(exception, "bank_accounts_account_number_key")) {
                    throw exception;
                }
                continue;
            }
            return bankAccountMapper.toResponse(savedAccount);
        }
        throw new DuplicateDataException("ACCOUNT_NUMBER_CONFLICT",
                "Не удалось подобрать свободный номер счёта. Повторите запрос");
    }

    public List<AccountResponse> getAccounts(String subject) {
        UserIdentity user = getUserBySubject(subject);

        return bankAccountRepository
                .findAllByUserId(user.id())
                .stream()
                .map(bankAccountMapper::toResponse)
                .toList();
    }

    public AccountResponse getAccount(Long accountId, String subject) {
        UserIdentity user = getUserBySubject(subject);

        BankAccount account = bankAccountRepository
                .findByIdAndUserId(accountId, user.id())
                .orElseThrow(() -> new NotFoundException("Счёт не найден"));

        return bankAccountMapper.toResponse(account);
    }

    private UserIdentity getUserBySubject(String subject) {
        return identityDirectory.findBySubject(subject)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }

}
