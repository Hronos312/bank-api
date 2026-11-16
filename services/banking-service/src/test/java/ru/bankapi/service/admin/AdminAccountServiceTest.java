package ru.bankapi.service.admin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.bankapi.dal.BankAccountRepository;
import ru.bankapi.dal.CardRepository;
import ru.bankapi.dto.account.AccountResponse;
import ru.bankapi.enums.AccountStatus;
import ru.bankapi.enums.CardStatus;
import ru.bankapi.exception.InvalidOperationException;
import ru.bankapi.exception.NotFoundException;
import ru.bankapi.mapper.BankAccountMapper;
import ru.bankapi.model.BankAccount;
import ru.bankapi.model.Card;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAccountServiceTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private BankAccountMapper bankAccountMapper;

    @InjectMocks
    private AdminAccountService adminAccountService;

    @Test
    void getAccountsShouldReturnAllAccounts() {
        BankAccount first = createAccount(
                1L,
                "100.00",
                AccountStatus.ACTIVE
        );

        BankAccount second = createAccount(
                2L,
                "0.00",
                AccountStatus.BLOCKED
        );

        AccountResponse firstResponse =
                new AccountResponse();

        firstResponse.setId(1L);

        AccountResponse secondResponse =
                new AccountResponse();

        secondResponse.setId(2L);

        when(bankAccountRepository.findAll())
                .thenReturn(List.of(first, second));

        when(bankAccountMapper.toResponse(first))
                .thenReturn(firstResponse);

        when(bankAccountMapper.toResponse(second))
                .thenReturn(secondResponse);

        List<AccountResponse> result =
                adminAccountService.getAccounts();

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
    }

    @Test
    void getAccountShouldReturnAccount() {
        BankAccount account = createAccount(
                1L,
                "100.00",
                AccountStatus.ACTIVE
        );

        AccountResponse response =
                new AccountResponse();

        response.setId(1L);

        when(bankAccountRepository.findById(1L))
                .thenReturn(Optional.of(account));

        when(bankAccountMapper.toResponse(account))
                .thenReturn(response);

        AccountResponse result =
                adminAccountService.getAccount(1L);

        assertEquals(1L, result.getId());
    }

    @Test
    void getAccountShouldThrowWhenAccountDoesNotExist() {
        when(bankAccountRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> adminAccountService.getAccount(99L)
        );
    }

    @Test
    void blockAccountShouldChangeStatusToBlocked() {
        BankAccount account = createAccount(
                1L,
                "100.00",
                AccountStatus.ACTIVE
        );

        AccountResponse response =
                new AccountResponse();

        response.setId(1L);
        response.setStatus(AccountStatus.BLOCKED);

        when(bankAccountRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(account));

        when(bankAccountMapper.toResponse(account))
                .thenReturn(response);

        AccountResponse result =
                adminAccountService.blockAccount(1L);

        assertEquals(
                AccountStatus.BLOCKED,
                account.getStatus()
        );

        assertEquals(
                AccountStatus.BLOCKED,
                result.getStatus()
        );

        verify(bankAccountRepository)
                .findByIdForUpdate(1L);
    }

    @Test
    void blockAccountShouldThrowWhenAlreadyBlocked() {
        BankAccount account = createAccount(
                1L,
                "100.00",
                AccountStatus.BLOCKED
        );

        when(bankAccountRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(account));

        assertThrows(
                InvalidOperationException.class,
                () -> adminAccountService.blockAccount(1L)
        );

        verify(bankAccountMapper, never())
                .toResponse(any(BankAccount.class));
    }

    @Test
    void blockAccountShouldThrowWhenAccountIsClosed() {
        BankAccount account = createAccount(
                1L,
                "0.00",
                AccountStatus.CLOSED
        );

        when(bankAccountRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(account));

        assertThrows(
                InvalidOperationException.class,
                () -> adminAccountService.blockAccount(1L)
        );
    }

    @Test
    void unblockAccountShouldChangeStatusToActive() {
        BankAccount account = createAccount(
                1L,
                "100.00",
                AccountStatus.BLOCKED
        );

        AccountResponse response =
                new AccountResponse();

        response.setId(1L);
        response.setStatus(AccountStatus.ACTIVE);

        when(bankAccountRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(account));

        when(bankAccountMapper.toResponse(account))
                .thenReturn(response);

        AccountResponse result =
                adminAccountService.unblockAccount(1L);

        assertEquals(
                AccountStatus.ACTIVE,
                account.getStatus()
        );

        assertEquals(
                AccountStatus.ACTIVE,
                result.getStatus()
        );
    }

    @Test
    void unblockAccountShouldThrowWhenAlreadyActive() {
        BankAccount account = createAccount(
                1L,
                "100.00",
                AccountStatus.ACTIVE
        );

        when(bankAccountRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(account));

        assertThrows(
                InvalidOperationException.class,
                () -> adminAccountService.unblockAccount(1L)
        );
    }

    @Test
    void unblockAccountShouldThrowWhenAccountIsClosed() {
        BankAccount account = createAccount(
                1L,
                "0.00",
                AccountStatus.CLOSED
        );

        when(bankAccountRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(account));

        assertThrows(
                InvalidOperationException.class,
                () -> adminAccountService.unblockAccount(1L)
        );
    }

    @Test
    void closeAccountShouldCloseZeroBalanceAccount() {
        BankAccount account = createAccount(
                1L,
                "0.00",
                AccountStatus.ACTIVE
        );

        AccountResponse response =
                new AccountResponse();

        response.setId(1L);
        response.setStatus(AccountStatus.CLOSED);

        when(bankAccountRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(account));

        when(cardRepository.findByBankAccountId(1L))
                .thenReturn(Optional.empty());

        when(bankAccountMapper.toResponse(account))
                .thenReturn(response);

        AccountResponse result =
                adminAccountService.closeAccount(1L);

        assertEquals(
                AccountStatus.CLOSED,
                account.getStatus()
        );

        assertEquals(
                AccountStatus.CLOSED,
                result.getStatus()
        );
    }

    @Test
    void closeAccountShouldCloseLinkedCard() {
        BankAccount account = createAccount(
                1L,
                "0.00",
                AccountStatus.ACTIVE
        );

        Card card = new Card();
        card.setId(10L);
        card.setBankAccount(account);
        card.setStatus(CardStatus.ACTIVE);

        AccountResponse response =
                new AccountResponse();

        response.setId(1L);
        response.setStatus(AccountStatus.CLOSED);

        when(bankAccountRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(account));

        when(cardRepository.findByBankAccountId(1L))
                .thenReturn(Optional.of(card));

        when(bankAccountMapper.toResponse(account))
                .thenReturn(response);

        adminAccountService.closeAccount(1L);

        assertEquals(
                AccountStatus.CLOSED,
                account.getStatus()
        );

        assertEquals(
                CardStatus.CLOSED,
                card.getStatus()
        );

        verify(cardRepository)
                .findByBankAccountId(1L);
    }

    @Test
    void closeAccountShouldThrowWhenBalanceIsNotZero() {
        BankAccount account = createAccount(
                1L,
                "100.00",
                AccountStatus.ACTIVE
        );

        when(bankAccountRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(account));

        assertThrows(
                InvalidOperationException.class,
                () -> adminAccountService.closeAccount(1L)
        );

        assertEquals(
                AccountStatus.ACTIVE,
                account.getStatus()
        );

        verify(cardRepository, never())
                .findByBankAccountId(anyLong());

        verify(bankAccountMapper, never())
                .toResponse(any(BankAccount.class));
    }

    @Test
    void closeAccountShouldThrowWhenAlreadyClosed() {
        BankAccount account = createAccount(
                1L,
                "0.00",
                AccountStatus.CLOSED
        );

        when(bankAccountRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(account));

        assertThrows(
                InvalidOperationException.class,
                () -> adminAccountService.closeAccount(1L)
        );

        verify(cardRepository, never())
                .findByBankAccountId(anyLong());
    }

    @Test
    void blockAccountShouldThrowWhenAccountDoesNotExist() {
        when(bankAccountRepository.findByIdForUpdate(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> adminAccountService.blockAccount(99L)
        );
    }

    private BankAccount createAccount(
            Long id,
            String balance,
            AccountStatus status
    ) {
        BankAccount account = new BankAccount();

        account.setId(id);
        account.setBalance(new BigDecimal(balance));
        account.setStatus(status);

        return account;
    }
}