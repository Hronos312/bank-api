package ru.bankapi.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankAccountServiceTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountNumberGenerator accountNumberGenerator;

    @Mock
    private BankAccountMapper bankAccountMapper;

    @InjectMocks
    private BankAccountService bankAccountService;

    @Test
    void createAccountShouldCreateActiveRubAccountWithZeroBalance() {
        String email = "ivan@example.com";

        User user = new User();
        user.setId(1L);
        user.setEmail(email);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(accountNumberGenerator.generate())
                .thenReturn("40817810000000000001");

        when(bankAccountRepository.existsByAccountNumber(
                "40817810000000000001"
        )).thenReturn(false);

        when(bankAccountRepository.save(any(BankAccount.class)))
                .thenAnswer(invocation -> {
                    BankAccount account =
                            invocation.getArgument(0);

                    account.setId(10L);
                    return account;
                });

        AccountResponse response = new AccountResponse();
        response.setId(10L);
        response.setAccountNumber("40817810000000000001");
        response.setBalance(BigDecimal.ZERO);
        response.setCurrency(CurrencyCode.RUB);
        response.setStatus(AccountStatus.ACTIVE);

        when(bankAccountMapper.toResponse(any(BankAccount.class)))
                .thenReturn(response);

        AccountResponse result =
                bankAccountService.createAccount(email);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals(
                "40817810000000000001",
                result.getAccountNumber()
        );
        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(result.getBalance())
        );
        assertEquals(CurrencyCode.RUB, result.getCurrency());
        assertEquals(AccountStatus.ACTIVE, result.getStatus());

        ArgumentCaptor<BankAccount> captor =
                ArgumentCaptor.forClass(BankAccount.class);

        verify(bankAccountRepository)
                .save(captor.capture());

        BankAccount savedAccount = captor.getValue();

        assertEquals(user, savedAccount.getUser());
        assertEquals(
                "40817810000000000001",
                savedAccount.getAccountNumber()
        );
        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(savedAccount.getBalance())
        );
        assertEquals(
                CurrencyCode.RUB,
                savedAccount.getCurrency()
        );
        assertEquals(
                AccountStatus.ACTIVE,
                savedAccount.getStatus()
        );
    }

    @Test
    void createAccountShouldGenerateAnotherNumberWhenFirstAlreadyExists() {
        String email = "ivan@example.com";

        User user = new User();
        user.setId(1L);
        user.setEmail(email);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(accountNumberGenerator.generate())
                .thenReturn(
                        "40817810000000000001",
                        "40817810000000000002"
                );

        when(bankAccountRepository.existsByAccountNumber(
                "40817810000000000001"
        )).thenReturn(true);

        when(bankAccountRepository.existsByAccountNumber(
                "40817810000000000002"
        )).thenReturn(false);

        when(bankAccountRepository.save(any(BankAccount.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        when(bankAccountMapper.toResponse(any(BankAccount.class)))
                .thenReturn(new AccountResponse());

        bankAccountService.createAccount(email);

        verify(accountNumberGenerator, times(2))
                .generate();

        verify(bankAccountRepository)
                .existsByAccountNumber(
                        "40817810000000000001"
                );

        verify(bankAccountRepository)
                .existsByAccountNumber(
                        "40817810000000000002"
                );
    }

    @Test
    void getAccountsShouldReturnOnlyCurrentUserAccounts() {
        String email = "ivan@example.com";

        User user = new User();
        user.setId(1L);
        user.setEmail(email);

        BankAccount firstAccount = new BankAccount();
        firstAccount.setId(10L);

        BankAccount secondAccount = new BankAccount();
        secondAccount.setId(11L);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(bankAccountRepository.findAllByUserId(1L))
                .thenReturn(
                        List.of(firstAccount, secondAccount)
                );

        AccountResponse firstResponse =
                new AccountResponse();
        firstResponse.setId(10L);

        AccountResponse secondResponse =
                new AccountResponse();
        secondResponse.setId(11L);

        when(bankAccountMapper.toResponse(firstAccount))
                .thenReturn(firstResponse);

        when(bankAccountMapper.toResponse(secondAccount))
                .thenReturn(secondResponse);

        List<AccountResponse> result =
                bankAccountService.getAccounts(email);

        assertEquals(2, result.size());
        assertEquals(10L, result.get(0).getId());
        assertEquals(11L, result.get(1).getId());

        verify(bankAccountRepository)
                .findAllByUserId(1L);
    }

    @Test
    void getAccountShouldReturnAccountOwnedByCurrentUser() {
        String email = "ivan@example.com";

        User user = new User();
        user.setId(1L);
        user.setEmail(email);

        BankAccount account = new BankAccount();
        account.setId(10L);
        account.setUser(user);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(bankAccountRepository.findByIdAndUserId(
                10L,
                1L
        )).thenReturn(Optional.of(account));

        AccountResponse response =
                new AccountResponse();
        response.setId(10L);

        when(bankAccountMapper.toResponse(account))
                .thenReturn(response);

        AccountResponse result =
                bankAccountService.getAccount(10L, email);

        assertEquals(10L, result.getId());

        verify(bankAccountRepository)
                .findByIdAndUserId(10L, 1L);
    }

    @Test
    void getAccountShouldThrowWhenAccountDoesNotBelongToCurrentUser() {
        String email = "ivan@example.com";

        User user = new User();
        user.setId(1L);
        user.setEmail(email);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(bankAccountRepository.findByIdAndUserId(
                99L,
                1L
        )).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> bankAccountService.getAccount(
                        99L,
                        email
                )
        );

        verify(bankAccountMapper, never())
                .toResponse(any(BankAccount.class));
    }
}