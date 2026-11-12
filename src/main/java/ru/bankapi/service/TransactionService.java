package ru.bankapi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bankapi.dal.BankAccountRepository;
import ru.bankapi.dal.BankTransactionRepository;
import ru.bankapi.dto.transaction.MoneyOperationRequest;
import ru.bankapi.dto.transaction.TransactionResponse;
import ru.bankapi.enums.AccountStatus;
import ru.bankapi.enums.TransactionType;
import ru.bankapi.enums.UserStatus;
import ru.bankapi.exception.InvalidOperationException;
import ru.bankapi.exception.NotFoundException;
import ru.bankapi.mapper.BankTransactionMapper;
import ru.bankapi.model.BankAccount;
import ru.bankapi.model.BankTransaction;
import ru.bankapi.identity.IdentityDirectory;
import ru.bankapi.identity.IdentityDirectory.UserIdentity;
import ru.bankapi.service.outbox.OutboxService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final BankTransactionRepository transactionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final IdentityDirectory identityDirectory;
    private final BankTransactionMapper transactionMapper;
    private final OutboxService outboxService;

    @Transactional
    public TransactionResponse deposit(Long accountId, String email, MoneyOperationRequest request) {
        UserIdentity user = getActiveUser(email);

        BankAccount account = getActiveAccount(accountId, user.id());

        account.setBalance(account.getBalance().add(request.getAmount()));

        BankTransaction transaction = new BankTransaction();

        transaction.setType(TransactionType.DEPOSIT);
        transaction.setDestinationAccount(account);
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());

        BankTransaction savedTransaction = transactionRepository.save(transaction);

        outboxService.recordBankTransaction(savedTransaction);

        return transactionMapper.toResponse(savedTransaction);
    }

    @Transactional
    public TransactionResponse withdraw(Long accountId, String email, MoneyOperationRequest request) {
        UserIdentity user = getActiveUser(email);

        BankAccount account = getActiveAccount(accountId, user.id());

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InvalidOperationException("Недостаточно средств");
        }

        account.setBalance(account.getBalance().subtract(request.getAmount()));

        BankTransaction transaction = new BankTransaction();

        transaction.setType(TransactionType.WITHDRAWAL);
        transaction.setSourceAccount(account);
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());

        BankTransaction savedTransaction = transactionRepository.save(transaction);

        outboxService.recordBankTransaction(savedTransaction);

        return transactionMapper.toResponse(savedTransaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionHistory(Long accountId, String email) {
        UserIdentity user = identityDirectory.findByUsername(email)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        bankAccountRepository.findByIdAndUserId(accountId, user.id())
                .orElseThrow(() -> new NotFoundException("Счёт не найден"));

        return transactionRepository
                .findAllBySourceAccountIdOrDestinationAccountIdOrderByCreatedAtDesc(accountId, accountId)
                .stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    private UserIdentity getActiveUser(String email) {
        UserIdentity user = identityDirectory.findByUsername(email)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        if (user.status() != UserStatus.ACTIVE) {
            throw new InvalidOperationException("Операции недоступны для заблокированного пользователя");
        }

        return user;
    }

    private BankAccount getActiveAccount(Long accountId, Long userId) {
        BankAccount account = bankAccountRepository
                .findByIdAndUserIdForUpdate(accountId, userId)
                .orElseThrow(() -> new NotFoundException("Счёт не найден"));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidOperationException("Операции доступны только для активного счёта");
        }

        return account;
    }
}