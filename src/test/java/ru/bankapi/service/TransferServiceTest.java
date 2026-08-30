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

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private BankTransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BankTransactionMapper transactionMapper;

    @InjectMocks
    private TransferService transferService;

    @Test
    void transferShouldMoveMoneyAndCreateTransferTransaction() {
        String email = "ivan@example.com";

        User user = createActiveUser(email);

        BankAccount source =
                createActiveAccount(
                        10L,
                        user,
                        "1000.00",
                        "40817810000000000001"
                );

        BankAccount destination =
                createActiveAccount(
                        20L,
                        new User(),
                        "300.00",
                        "40817810000000000002"
                );

        TransferRequest request = createRequest(
                "40817810000000000002",
                "400.00"
        );
        request.setDescription("Перевод");

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(bankAccountRepository.findByIdAndUserId(
                10L,
                1L
        )).thenReturn(Optional.of(source));

        when(bankAccountRepository.findByAccountNumber(
                "40817810000000000002"
        )).thenReturn(Optional.of(destination));

        when(transactionRepository.save(any(BankTransaction.class)))
                .thenAnswer(invocation -> {
                    BankTransaction transaction =
                            invocation.getArgument(0);

                    transaction.setId(30L);
                    return transaction;
                });

        TransactionResponse response =
                new TransactionResponse();

        response.setId(30L);
        response.setType(TransactionType.TRANSFER);
        response.setSourceAccountId(10L);
        response.setDestinationAccountId(20L);
        response.setAmount(new BigDecimal("400.00"));

        when(transactionMapper.toResponse(
                any(BankTransaction.class)
        )).thenReturn(response);

        TransactionResponse result =
                transferService.transfer(
                        10L,
                        email,
                        request
                );

        assertEquals(
                0,
                new BigDecimal("600.00")
                        .compareTo(source.getBalance())
        );

        assertEquals(
                0,
                new BigDecimal("700.00")
                        .compareTo(destination.getBalance())
        );

        assertEquals(
                TransactionType.TRANSFER,
                result.getType()
        );

        ArgumentCaptor<BankTransaction> captor =
                ArgumentCaptor.forClass(
                        BankTransaction.class
                );

        verify(transactionRepository)
                .save(captor.capture());

        BankTransaction transaction =
                captor.getValue();

        assertEquals(
                TransactionType.TRANSFER,
                transaction.getType()
        );

        assertEquals(
                source,
                transaction.getSourceAccount()
        );

        assertEquals(
                destination,
                transaction.getDestinationAccount()
        );

        assertEquals(
                0,
                new BigDecimal("400.00")
                        .compareTo(transaction.getAmount())
        );

        assertEquals(
                "Перевод",
                transaction.getDescription()
        );
    }

    @Test
    void transferShouldThrowWhenBalanceIsInsufficient() {
        String email = "ivan@example.com";

        User user = createActiveUser(email);

        BankAccount source =
                createActiveAccount(
                        10L,
                        user,
                        "100.00",
                        "40817810000000000001"
                );

        BankAccount destination =
                createActiveAccount(
                        20L,
                        new User(),
                        "300.00",
                        "40817810000000000002"
                );

        TransferRequest request = createRequest(
                "40817810000000000002",
                "500.00"
        );

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(bankAccountRepository.findByIdAndUserId(
                10L,
                1L
        )).thenReturn(Optional.of(source));

        when(bankAccountRepository.findByAccountNumber(
                "40817810000000000002"
        )).thenReturn(Optional.of(destination));

        assertThrows(
                InvalidOperationException.class,
                () -> transferService.transfer(
                        10L,
                        email,
                        request
                )
        );

        assertEquals(
                0,
                new BigDecimal("100.00")
                        .compareTo(source.getBalance())
        );

        assertEquals(
                0,
                new BigDecimal("300.00")
                        .compareTo(destination.getBalance())
        );

        verify(transactionRepository, never())
                .save(any(BankTransaction.class));
    }

    @Test
    void transferShouldThrowWhenSourceAccountDoesNotBelongToUser() {
        String email = "ivan@example.com";

        User user = createActiveUser(email);

        TransferRequest request = createRequest(
                "40817810000000000002",
                "100.00"
        );

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(bankAccountRepository.findByIdAndUserId(
                99L,
                1L
        )).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> transferService.transfer(
                        99L,
                        email,
                        request
                )
        );

        verify(
                bankAccountRepository,
                never()
        ).findByAccountNumber(anyString());

        verify(transactionRepository, never())
                .save(any(BankTransaction.class));
    }

    @Test
    void transferShouldThrowWhenDestinationAccountDoesNotExist() {
        String email = "ivan@example.com";

        User user = createActiveUser(email);

        BankAccount source =
                createActiveAccount(
                        10L,
                        user,
                        "1000.00",
                        "40817810000000000001"
                );

        TransferRequest request = createRequest(
                "40817810000000000099",
                "100.00"
        );

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(bankAccountRepository.findByIdAndUserId(
                10L,
                1L
        )).thenReturn(Optional.of(source));

        when(bankAccountRepository.findByAccountNumber(
                "40817810000000000099"
        )).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> transferService.transfer(
                        10L,
                        email,
                        request
                )
        );

        verify(transactionRepository, never())
                .save(any(BankTransaction.class));
    }

    @Test
    void transferShouldThrowWhenDestinationAccountIsBlocked() {
        String email = "ivan@example.com";

        User user = createActiveUser(email);

        BankAccount source =
                createActiveAccount(
                        10L,
                        user,
                        "1000.00",
                        "40817810000000000001"
                );

        BankAccount destination =
                createActiveAccount(
                        20L,
                        new User(),
                        "300.00",
                        "40817810000000000002"
                );

        destination.setStatus(AccountStatus.BLOCKED);

        TransferRequest request = createRequest(
                "40817810000000000002",
                "100.00"
        );

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(bankAccountRepository.findByIdAndUserId(
                10L,
                1L
        )).thenReturn(Optional.of(source));

        when(bankAccountRepository.findByAccountNumber(
                "40817810000000000002"
        )).thenReturn(Optional.of(destination));

        assertThrows(
                InvalidOperationException.class,
                () -> transferService.transfer(
                        10L,
                        email,
                        request
                )
        );

        verify(transactionRepository, never())
                .save(any(BankTransaction.class));
    }

    @Test
    void transferShouldThrowWhenSourceAndDestinationAreSameAccount() {
        String email = "ivan@example.com";

        User user = createActiveUser(email);

        BankAccount account =
                createActiveAccount(
                        10L,
                        user,
                        "1000.00",
                        "40817810000000000001"
                );

        TransferRequest request = createRequest(
                "40817810000000000001",
                "100.00"
        );

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(bankAccountRepository.findByIdAndUserId(
                10L,
                1L
        )).thenReturn(Optional.of(account));

        when(bankAccountRepository.findByAccountNumber(
                "40817810000000000001"
        )).thenReturn(Optional.of(account));

        assertThrows(
                InvalidOperationException.class,
                () -> transferService.transfer(
                        10L,
                        email,
                        request
                )
        );

        assertEquals(
                0,
                new BigDecimal("1000.00")
                        .compareTo(account.getBalance())
        );

        verify(transactionRepository, never())
                .save(any(BankTransaction.class));
    }

    @Test
    void transferShouldThrowWhenUserIsBlocked() {
        String email = "ivan@example.com";

        User user = createActiveUser(email);
        user.setStatus(UserStatus.BLOCKED);

        TransferRequest request = createRequest(
                "40817810000000000002",
                "100.00"
        );

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        assertThrows(
                InvalidOperationException.class,
                () -> transferService.transfer(
                        10L,
                        email,
                        request
                )
        );

        verify(
                bankAccountRepository,
                never()
        ).findByIdAndUserId(anyLong(), anyLong());

        verify(transactionRepository, never())
                .save(any(BankTransaction.class));
    }

    private User createActiveUser(String email) {
        User user = new User();

        user.setId(1L);
        user.setEmail(email);
        user.setStatus(UserStatus.ACTIVE);

        return user;
    }

    private BankAccount createActiveAccount(
            Long id,
            User user,
            String balance,
            String accountNumber
    ) {
        BankAccount account = new BankAccount();

        account.setId(id);
        account.setUser(user);
        account.setBalance(new BigDecimal(balance));
        account.setAccountNumber(accountNumber);
        account.setStatus(AccountStatus.ACTIVE);

        return account;
    }

    private TransferRequest createRequest(
            String destinationAccountNumber,
            String amount
    ) {
        TransferRequest request =
                new TransferRequest();

        request.setDestinationAccountNumber(
                destinationAccountNumber
        );
        request.setAmount(new BigDecimal(amount));

        return request;
    }
}