package ru.bankapi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bankapi.dal.BankAccountRepository;
import ru.bankapi.dal.BankTransactionRepository;
import ru.bankapi.dal.UserRepository;
import ru.bankapi.dto.transaction.TransactionResponse;
import ru.bankapi.dto.transfer.TransferRequest;
import ru.bankapi.enums.AccountStatus;
import ru.bankapi.enums.TransactionType;
import ru.bankapi.enums.UserStatus;
import ru.bankapi.exception.InvalidOperationException;
import ru.bankapi.exception.NotFoundException;
import ru.bankapi.mapper.BankTransactionMapper;
import ru.bankapi.model.BankAccount;
import ru.bankapi.model.BankTransaction;
import ru.bankapi.model.User;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final BankAccountRepository bankAccountRepository;
    private final BankTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final BankTransactionMapper transactionMapper;

    @Transactional
    public TransactionResponse transfer(Long sourceAccountId, String email, TransferRequest request) {
        User user = getActiveUser(email);

        Long destinationAccountId = bankAccountRepository.findIdByAccountNumber(request.getDestinationAccountNumber())
                .orElseThrow(() -> new NotFoundException("Счёт получателя не найден"));

        if (sourceAccountId.equals(destinationAccountId)) {
            throw new InvalidOperationException("Нельзя перевести деньги на тот же счёт");
        }

        Long firstAccountId = Math.min(sourceAccountId, destinationAccountId);

        Long secondAccountId = Math.max(sourceAccountId, destinationAccountId);

        BankAccount firstAccount = bankAccountRepository.findByIdForUpdate(firstAccountId)
                .orElseThrow(() -> new NotFoundException("Счёт не найден"));

        BankAccount secondAccount = bankAccountRepository.findByIdForUpdate(secondAccountId)
                .orElseThrow(() -> new NotFoundException("Счёт не найден"));

        BankAccount sourceAccount = sourceAccountId.equals(firstAccountId) ? firstAccount : secondAccount;

        BankAccount destinationAccount = destinationAccountId.equals(firstAccountId) ? firstAccount : secondAccount;

        if (!sourceAccount.getUser().getId().equals(user.getId())) {
            throw new NotFoundException("Счёт отправителя не найден");
        }

        if (sourceAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidOperationException("Переводы доступны только с активного счёта");
        }

        if (destinationAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidOperationException("Счёт получателя недоступен для переводов");
        }

        if (sourceAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InvalidOperationException("Недостаточно средств");
        }

        sourceAccount.setBalance(sourceAccount.getBalance().subtract(request.getAmount()));

        destinationAccount.setBalance(destinationAccount.getBalance().add(request.getAmount()));

        BankTransaction transaction = new BankTransaction();

        transaction.setType(TransactionType.TRANSFER);
        transaction.setSourceAccount(sourceAccount);
        transaction.setDestinationAccount(destinationAccount);
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());

        BankTransaction savedTransaction = transactionRepository.save(transaction);

        return transactionMapper.toResponse(savedTransaction);
    }

    private User getActiveUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidOperationException("Операции недоступны для заблокированного пользователя");
        }

        return user;
    }
}