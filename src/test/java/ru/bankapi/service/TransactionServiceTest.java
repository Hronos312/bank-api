package ru.bankapi.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.bankapi.dal.BankAccountRepository;
import ru.bankapi.dal.BankTransactionRepository;
import ru.bankapi.dal.UserRepository;
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
import ru.bankapi.model.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private BankTransactionRepository transactionRepository;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BankTransactionMapper transactionMapper;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void depositShouldIncreaseBalanceAndCreateDepositTransaction() {
        String email = "ivan@example.com";

        User user = createActiveUser(email);
        BankAccount account = createActiveAccount(user, "1000.00");

        MoneyOperationRequest request = new MoneyOperationRequest();
        request.setAmount(new BigDecimal("500.00"));
        request.setDescription("Пополнение");

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(bankAccountRepository.findByIdAndUserIdForUpdate(10L, 1L))
                .thenReturn(Optional.of(account));

        when(transactionRepository.save(any(BankTransaction.class)))
                .thenAnswer(invocation -> {
                    BankTransaction transaction = invocation.getArgument(0);
                    transaction.setId(20L);
                    return transaction;
                });

        TransactionResponse response = new TransactionResponse();
        response.setId(20L);
        response.setType(TransactionType.DEPOSIT);
        response.setDestinationAccountId(10L);
        response.setAmount(new BigDecimal("500.00"));

        when(transactionMapper.toResponse(any(BankTransaction.class)))
                .thenReturn(response);

        TransactionResponse result = transactionService.deposit(
                10L,
                email,
                request
        );

        assertEquals(
                0,
                new BigDecimal("1500.00")
                        .compareTo(account.getBalance())
        );

        assertEquals(TransactionType.DEPOSIT, result.getType());

        ArgumentCaptor<BankTransaction> captor =
                ArgumentCaptor.forClass(BankTransaction.class);

        verify(transactionRepository)
                .save(captor.capture());

        BankTransaction transaction = captor.getValue();

        assertEquals(
                TransactionType.DEPOSIT,
                transaction.getType()
        );

        assertNull(transaction.getSourceAccount());

        assertEquals(
                account,
                transaction.getDestinationAccount()
        );

        assertEquals(
                0,
                new BigDecimal("500.00")
                        .compareTo(transaction.getAmount())
        );

        assertEquals(
                "Пополнение",
                transaction.getDescription()
        );

        verify(bankAccountRepository)
                .findByIdAndUserIdForUpdate(10L, 1L);
    }

    @Test
    void withdrawShouldDecreaseBalanceAndCreateWithdrawalTransaction() {
        String email = "ivan@example.com";

        User user = createActiveUser(email);
        BankAccount account = createActiveAccount(user, "1000.00");

        MoneyOperationRequest request = new MoneyOperationRequest();
        request.setAmount(new BigDecimal("250.00"));
        request.setDescription("Снятие");

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(bankAccountRepository.findByIdAndUserIdForUpdate(10L, 1L))
                .thenReturn(Optional.of(account));

        when(transactionRepository.save(any(BankTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(transactionMapper.toResponse(any(BankTransaction.class)))
                .thenReturn(new TransactionResponse());

        transactionService.withdraw(
                10L,
                email,
                request
        );

        assertEquals(
                0,
                new BigDecimal("750.00")
                        .compareTo(account.getBalance())
        );

        ArgumentCaptor<BankTransaction> captor =
                ArgumentCaptor.forClass(BankTransaction.class);

        verify(transactionRepository)
                .save(captor.capture());

        BankTransaction transaction = captor.getValue();

        assertEquals(
                TransactionType.WITHDRAWAL,
                transaction.getType()
        );

        assertEquals(
                account,
                transaction.getSourceAccount()
        );

        assertNull(transaction.getDestinationAccount());

        assertEquals(
                0,
                new BigDecimal("250.00")
                        .compareTo(transaction.getAmount())
        );

        assertEquals(
                "Снятие",
                transaction.getDescription()
        );

        verify(bankAccountRepository)
                .findByIdAndUserIdForUpdate(10L, 1L);
    }

    @Test
    void withdrawShouldThrowWhenBalanceIsInsufficient() {
        String email = "ivan@example.com";

        User user = createActiveUser(email);
        BankAccount account = createActiveAccount(user, "100.00");

        MoneyOperationRequest request = new MoneyOperationRequest();
        request.setAmount(new BigDecimal("500.00"));

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(bankAccountRepository.findByIdAndUserIdForUpdate(10L, 1L))
                .thenReturn(Optional.of(account));

        assertThrows(
                InvalidOperationException.class,
                () -> transactionService.withdraw(
                        10L,
                        email,
                        request
                )
        );

        assertEquals(
                0,
                new BigDecimal("100.00")
                        .compareTo(account.getBalance())
        );

        verify(transactionRepository, never())
                .save(any(BankTransaction.class));
    }

    @Test
    void depositShouldThrowWhenUserIsBlocked() {
        String email = "ivan@example.com";

        User user = createActiveUser(email);
        user.setStatus(UserStatus.BLOCKED);

        MoneyOperationRequest request = new MoneyOperationRequest();
        request.setAmount(new BigDecimal("100.00"));

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        assertThrows(
                InvalidOperationException.class,
                () -> transactionService.deposit(
                        10L,
                        email,
                        request
                )
        );

        verify(bankAccountRepository, never())
                .findByIdAndUserIdForUpdate(anyLong(), anyLong());

        verify(transactionRepository, never())
                .save(any(BankTransaction.class));
    }

    @Test
    void depositShouldThrowWhenAccountIsBlocked() {
        String email = "ivan@example.com";

        User user = createActiveUser(email);
        BankAccount account = createActiveAccount(user, "1000.00");
        account.setStatus(AccountStatus.BLOCKED);

        MoneyOperationRequest request = new MoneyOperationRequest();
        request.setAmount(new BigDecimal("100.00"));

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(bankAccountRepository.findByIdAndUserIdForUpdate(10L, 1L))
                .thenReturn(Optional.of(account));

        assertThrows(
                InvalidOperationException.class,
                () -> transactionService.deposit(
                        10L,
                        email,
                        request
                )
        );

        verify(transactionRepository, never())
                .save(any(BankTransaction.class));
    }

    @Test
    void depositShouldThrowWhenAccountDoesNotBelongToCurrentUser() {
        String email = "ivan@example.com";

        User user = createActiveUser(email);

        MoneyOperationRequest request = new MoneyOperationRequest();
        request.setAmount(new BigDecimal("100.00"));

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(bankAccountRepository.findByIdAndUserIdForUpdate(99L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> transactionService.deposit(
                        99L,
                        email,
                        request
                )
        );

        verify(transactionRepository, never())
                .save(any(BankTransaction.class));
    }

    @Test
    void getTransactionHistoryShouldReturnCurrentUserAccountTransactions() {
        String email = "ivan@example.com";

        User user = createActiveUser(email);
        BankAccount account = createActiveAccount(user, "1000.00");

        BankTransaction firstTransaction = new BankTransaction();
        firstTransaction.setId(20L);
        firstTransaction.setType(TransactionType.WITHDRAWAL);

        BankTransaction secondTransaction = new BankTransaction();
        secondTransaction.setId(19L);
        secondTransaction.setType(TransactionType.DEPOSIT);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(bankAccountRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(account));

        when(transactionRepository
                .findAllBySourceAccountIdOrDestinationAccountIdOrderByCreatedAtDesc(
                        10L,
                        10L
                ))
                .thenReturn(List.of(
                        firstTransaction,
                        secondTransaction
                ));

        TransactionResponse firstResponse =
                new TransactionResponse();

        firstResponse.setId(20L);
        firstResponse.setType(TransactionType.WITHDRAWAL);

        TransactionResponse secondResponse =
                new TransactionResponse();

        secondResponse.setId(19L);
        secondResponse.setType(TransactionType.DEPOSIT);

        when(transactionMapper.toResponse(firstTransaction))
                .thenReturn(firstResponse);

        when(transactionMapper.toResponse(secondTransaction))
                .thenReturn(secondResponse);

        List<TransactionResponse> result =
                transactionService.getTransactionHistory(
                        10L,
                        email
                );

        assertEquals(2, result.size());

        assertEquals(
                20L,
                result.get(0).getId()
        );

        assertEquals(
                TransactionType.WITHDRAWAL,
                result.get(0).getType()
        );

        assertEquals(
                19L,
                result.get(1).getId()
        );

        assertEquals(
                TransactionType.DEPOSIT,
                result.get(1).getType()
        );

        verify(bankAccountRepository)
                .findByIdAndUserId(10L, 1L);

        verify(transactionRepository)
                .findAllBySourceAccountIdOrDestinationAccountIdOrderByCreatedAtDesc(
                        10L,
                        10L
                );
    }

    @Test
    void getTransactionHistoryShouldReturnEmptyListWhenAccountHasNoTransactions() {
        String email = "ivan@example.com";

        User user = createActiveUser(email);
        BankAccount account = createActiveAccount(user, "0.00");

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(bankAccountRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(account));

        when(transactionRepository
                .findAllBySourceAccountIdOrDestinationAccountIdOrderByCreatedAtDesc(
                        10L,
                        10L
                ))
                .thenReturn(List.of());

        List<TransactionResponse> result =
                transactionService.getTransactionHistory(
                        10L,
                        email
                );

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(transactionRepository)
                .findAllBySourceAccountIdOrDestinationAccountIdOrderByCreatedAtDesc(
                        10L,
                        10L
                );
    }

    @Test
    void getTransactionHistoryShouldThrowWhenAccountDoesNotBelongToCurrentUser() {
        String email = "ivan@example.com";

        User user = createActiveUser(email);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(bankAccountRepository.findByIdAndUserId(
                99L,
                1L
        )).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> transactionService
                        .getTransactionHistory(
                                99L,
                                email
                        )
        );

        verify(transactionRepository, never())
                .findAllBySourceAccountIdOrDestinationAccountIdOrderByCreatedAtDesc(
                        anyLong(),
                        anyLong()
                );
    }

    private User createActiveUser(String email) {
        User user = new User();

        user.setId(1L);
        user.setEmail(email);
        user.setStatus(UserStatus.ACTIVE);

        return user;
    }

    private BankAccount createActiveAccount(
            User user,
            String balance
    ) {
        BankAccount account = new BankAccount();

        account.setId(10L);
        account.setUser(user);
        account.setBalance(new BigDecimal(balance));
        account.setStatus(AccountStatus.ACTIVE);

        return account;
    }
}