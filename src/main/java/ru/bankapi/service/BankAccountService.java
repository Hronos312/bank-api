package ru.bankapi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bankapi.dal.BankAccountRepository;
import ru.bankapi.dal.UserRepository;
import ru.bankapi.dto.account.AccountResponse;
import ru.bankapi.enums.AccountStatus;
import ru.bankapi.enums.CurrencyCode;
import ru.bankapi.exception.NotFoundException;
import ru.bankapi.generator.AccountNumberGenerator;
import ru.bankapi.mapper.BankAccountMapper;
import ru.bankapi.model.BankAccount;
import ru.bankapi.model.User;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;
    private final AccountNumberGenerator accountNumberGenerator;
    private final BankAccountMapper bankAccountMapper;

    public AccountResponse createAccount(String email) {
        User user = getUserByEmail(email);

        BankAccount account = new BankAccount();

        account.setAccountNumber(generateUniqueAccountNumber());
        account.setUser(user);
        account.setBalance(BigDecimal.ZERO);
        account.setCurrency(CurrencyCode.RUB);
        account.setStatus(AccountStatus.ACTIVE);

        BankAccount savedAccount = bankAccountRepository.save(account);

        return bankAccountMapper.toResponse(savedAccount);
    }

    public List<AccountResponse> getAccounts(String email) {
        User user = getUserByEmail(email);

        return bankAccountRepository
                .findAllByUserId(user.getId())
                .stream()
                .map(bankAccountMapper::toResponse)
                .toList();
    }

    public AccountResponse getAccount(Long accountId, String email) {
        User user = getUserByEmail(email);

        BankAccount account = bankAccountRepository
                .findByIdAndUserId(accountId, user.getId())
                .orElseThrow(() -> new NotFoundException("Счёт не найден"));

        return bankAccountMapper.toResponse(account);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }

    private String generateUniqueAccountNumber() {
        String accountNumber;

        do {
            accountNumber = accountNumberGenerator.generate();
        } while (
                bankAccountRepository.existsByAccountNumber(accountNumber)
        );

        return accountNumber;
    }
}