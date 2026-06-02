package pk.la.pasir_lech_artur.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pk.la.pasir_lech_artur.dto.BalanceDTO;
import pk.la.pasir_lech_artur.dto.TransactionDTO;
import pk.la.pasir_lech_artur.model.Transaction;
import pk.la.pasir_lech_artur.model.TransactionType;
import pk.la.pasir_lech_artur.model.User;
import pk.la.pasir_lech_artur.repository.TransactionRepository;
import pk.la.pasir_lech_artur.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public TransactionService(TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    public List<Transaction> getAllTransactions() {
        User user = getCurrentUser();
        return transactionRepository.findAllByUser(user);
    }

    public Transaction getTransactionById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono transakcji o ID " + id));

        if (!transaction.getUser().getEmail().equals(getCurrentUser().getEmail())) {
            throw new AccessDeniedException("Nie masz dostępu do tej transakcji");
        }

        return transaction;
    }

    public Transaction updateTransaction(Long id, TransactionDTO transactionDTO) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono transakcji o ID " + id));

        if (!transaction.getUser().getEmail().equals(getCurrentUser().getEmail())) {
            throw new AccessDeniedException("Nie masz dostępu do tej transakcji");
        }

        transaction.setAmount(transactionDTO.getAmount());
        transaction.setType(TransactionType.valueOf(transactionDTO.getType()));
        transaction.setTags(transactionDTO.getTags());
        transaction.setNotes(transactionDTO.getNotes());

        return transactionRepository.save(transaction);
    }

    public Transaction createTransaction(TransactionDTO transactionDTO) {
        Transaction transaction = new Transaction(
                transactionDTO.getAmount(),
                TransactionType.valueOf(transactionDTO.getType()),
                transactionDTO.getTags(),
                transactionDTO.getNotes(),
                getCurrentUser()
        );

        return transactionRepository.save(transaction);
    }

    public Transaction createSystemTransaction(
            User user,
            Double amount,
            TransactionType type,
            String tags,
            String notes) {

        Transaction transaction = new Transaction(
                amount,
                type,
                tags,
                notes,
                user
        );

        return transactionRepository.save(transaction);
    }

    public void deleteTransaction(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono transakcji o ID " + id));

        if (!transaction.getUser().getEmail().equals(getCurrentUser().getEmail())) {
            throw new AccessDeniedException("Nie masz dostępu do tej transakcji");
        }

        transactionRepository.delete(transaction);
    }

    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Użytkownik nie jest uwierzytelniony");
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono zalogowanego użytkownika: " + email));
    }

    public BalanceDTO getCurrentUserBalance(Integer days) {
        return getCurrentUserBalance(days, null, null);
    }

    public BalanceDTO getCurrentUserBalance(Integer days, LocalDate from, LocalDate to) {
        User user = getCurrentUser();
        return getUserBalance(user, days, from, to);
    }

    public BalanceDTO getUserBalance(User user, Integer days) {
        return getUserBalance(user, days, null, null);
    }

    public BalanceDTO getUserBalance(User user, Integer days, LocalDate from, LocalDate to) {
        List<Transaction> userTransactions;

        if (from != null || to != null) {
            LocalDateTime fromDateTime = from != null
                    ? from.atStartOfDay()
                    : LocalDate.of(1970, 1, 1).atStartOfDay();

            LocalDateTime toDateTime = to != null
                    ? to.atTime(LocalTime.MAX)
                    : LocalDateTime.now();

            if (fromDateTime.isAfter(toDateTime)) {
                throw new IllegalArgumentException("Data początkowa nie może być późniejsza niż data końcowa.");
            }

            userTransactions = transactionRepository
                    .findAllByUserAndTimestampGreaterThanEqualAndTimestampLessThanEqual(
                            user,
                            fromDateTime,
                            toDateTime
                    );
        } else if (days != null) {
            if (days < 0) {
                throw new IllegalArgumentException("Liczba dni nie może być ujemna.");
            }

            LocalDateTime fromDate = LocalDateTime.now().minusDays(days);
            userTransactions = transactionRepository.findAllByUserAndTimestampGreaterThanEqual(user, fromDate);
        } else {
            userTransactions = transactionRepository.findByUser(user);
        }

        double income = userTransactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .mapToDouble(Transaction::getAmount)
                .sum();

        double expense = userTransactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .mapToDouble(Transaction::getAmount)
                .sum();

        return new BalanceDTO(income, expense, income - expense);
    }
}