package ru.bankapi.service.admin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.bankapi.dal.BankTransactionRepository;
import ru.bankapi.dto.transaction.TransactionPageResponse;
import ru.bankapi.dto.transaction.TransactionResponse;
import ru.bankapi.enums.TransactionType;
import ru.bankapi.exception.BadRequestException;
import ru.bankapi.exception.NotFoundException;
import ru.bankapi.mapper.BankTransactionMapper;
import ru.bankapi.model.BankTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminTransactionServiceTest {

    @Mock
    private BankTransactionRepository transactionRepository;

    @Mock
    private BankTransactionMapper transactionMapper;

    @InjectMocks
    private AdminTransactionService adminTransactionService;

    @Test
    void getTransactionsShouldReturnPageResponse() {
        BankTransaction first = createTransaction(
                2L,
                TransactionType.TRANSFER,
                "500.00"
        );

        BankTransaction second = createTransaction(
                1L,
                TransactionType.WITHDRAWAL,
                "100.00"
        );

        Page<BankTransaction> page =
                new PageImpl<>(
                        List.of(first, second),
                        org.springframework.data.domain.PageRequest.of(
                                0,
                                2
                        ),
                        5
                );

        TransactionResponse firstResponse =
                createResponse(
                        2L,
                        TransactionType.TRANSFER,
                        "500.00"
                );

        TransactionResponse secondResponse =
                createResponse(
                        1L,
                        TransactionType.WITHDRAWAL,
                        "100.00"
                );

        when(transactionRepository.findAll(
                any(Pageable.class)
        )).thenReturn(page);

        when(transactionMapper.toResponse(first))
                .thenReturn(firstResponse);

        when(transactionMapper.toResponse(second))
                .thenReturn(secondResponse);

        TransactionPageResponse result =
                adminTransactionService
                        .getTransactions(0, 2);

        assertEquals(2, result.getContent().size());
        assertEquals(0, result.getPage());
        assertEquals(2, result.getSize());
        assertEquals(5, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
        assertTrue(result.isFirst());
        assertFalse(result.isLast());

        assertEquals(
                2L,
                result.getContent()
                        .get(0)
                        .getId()
        );

        assertEquals(
                1L,
                result.getContent()
                        .get(1)
                        .getId()
        );
    }

    @Test
    void getTransactionsShouldUseCreatedAtDescendingSort() {
        Page<BankTransaction> page =
                new PageImpl<>(List.of());

        when(transactionRepository.findAll(
                any(Pageable.class)
        )).thenReturn(page);

        adminTransactionService
                .getTransactions(2, 25);

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(
                        Pageable.class
                );

        verify(transactionRepository)
                .findAll(captor.capture());

        Pageable pageable =
                captor.getValue();

        assertEquals(
                2,
                pageable.getPageNumber()
        );

        assertEquals(
                25,
                pageable.getPageSize()
        );

        assertNotNull(
                pageable.getSort()
                        .getOrderFor("createdAt")
        );

        assertTrue(
                pageable.getSort()
                        .getOrderFor("createdAt")
                        .isDescending()
        );
    }

    @Test
    void getTransactionShouldReturnTransaction() {
        BankTransaction transaction =
                createTransaction(
                        1L,
                        TransactionType.DEPOSIT,
                        "1000.00"
                );

        TransactionResponse response =
                createResponse(
                        1L,
                        TransactionType.DEPOSIT,
                        "1000.00"
                );

        when(transactionRepository.findById(1L))
                .thenReturn(
                        Optional.of(transaction)
                );

        when(transactionMapper.toResponse(
                transaction
        )).thenReturn(response);

        TransactionResponse result =
                adminTransactionService
                        .getTransaction(1L);

        assertEquals(
                1L,
                result.getId()
        );

        assertEquals(
                TransactionType.DEPOSIT,
                result.getType()
        );
    }

    @Test
    void getTransactionShouldThrowWhenTransactionDoesNotExist() {
        when(transactionRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> adminTransactionService
                        .getTransaction(99L)
        );
    }

    @Test
    void getTransactionsShouldThrowWhenPageIsNegative() {
        assertThrows(
                BadRequestException.class,
                () -> adminTransactionService
                        .getTransactions(-1, 20)
        );

        verify(transactionRepository, never())
                .findAll(any(Pageable.class));
    }

    @Test
    void getTransactionsShouldThrowWhenSizeIsZero() {
        assertThrows(
                BadRequestException.class,
                () -> adminTransactionService
                        .getTransactions(0, 0)
        );

        verify(transactionRepository, never())
                .findAll(any(Pageable.class));
    }

    @Test
    void getTransactionsShouldThrowWhenSizeIsGreaterThanMaximum() {
        assertThrows(
                BadRequestException.class,
                () -> adminTransactionService
                        .getTransactions(0, 101)
        );

        verify(transactionRepository, never())
                .findAll(any(Pageable.class));
    }

    @Test
    void getTransactionsShouldAllowMaximumPageSize() {
        Page<BankTransaction> page =
                new PageImpl<>(List.of());

        when(transactionRepository.findAll(
                any(Pageable.class)
        )).thenReturn(page);

        assertDoesNotThrow(
                () -> adminTransactionService
                        .getTransactions(0, 100)
        );
    }

    private BankTransaction createTransaction(
            Long id,
            TransactionType type,
            String amount
    ) {
        BankTransaction transaction =
                new BankTransaction();

        transaction.setId(id);
        transaction.setType(type);
        transaction.setAmount(
                new BigDecimal(amount)
        );
        transaction.setCreatedAt(
                LocalDateTime.now()
        );

        return transaction;
    }

    private TransactionResponse createResponse(
            Long id,
            TransactionType type,
            String amount
    ) {
        TransactionResponse response =
                new TransactionResponse();

        response.setId(id);
        response.setType(type);
        response.setAmount(
                new BigDecimal(amount)
        );

        return response;
    }
}