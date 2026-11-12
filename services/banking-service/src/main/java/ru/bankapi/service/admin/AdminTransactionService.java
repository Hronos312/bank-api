package ru.bankapi.service.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bankapi.dal.BankTransactionRepository;
import ru.bankapi.dto.transaction.TransactionPageResponse;
import ru.bankapi.dto.transaction.TransactionResponse;
import ru.bankapi.exception.BadRequestException;
import ru.bankapi.exception.NotFoundException;
import ru.bankapi.mapper.BankTransactionMapper;
import ru.bankapi.model.BankTransaction;

@Service
@RequiredArgsConstructor
public class AdminTransactionService {

    private static final int MAX_PAGE_SIZE = 100;

    private final BankTransactionRepository transactionRepository;
    private final BankTransactionMapper transactionMapper;

    @Transactional(readOnly = true)
    public TransactionPageResponse getTransactions(int page, int size) {
        validatePagination(page, size);

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<BankTransaction> transactions = transactionRepository.findAll(pageRequest);

        TransactionPageResponse response = new TransactionPageResponse();

        response.setContent(transactions.getContent()
                        .stream()
                        .map(transactionMapper::toResponse)
                        .toList()
        );

        response.setPage(transactions.getNumber());

        response.setSize(transactions.getSize());

        response.setTotalElements(transactions.getTotalElements());

        response.setTotalPages(transactions.getTotalPages());

        response.setFirst(transactions.isFirst());

        response.setLast(transactions.isLast());

        return response;
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(Long transactionId) {
        BankTransaction transaction = transactionRepository
                        .findById(transactionId)
                        .orElseThrow(() -> new NotFoundException("Транзакция не найдена"));

        return transactionMapper.toResponse(transaction);
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Номер страницы не может быть отрицательным");
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("Размер страницы должен быть от 1 до 100");
        }
    }
}