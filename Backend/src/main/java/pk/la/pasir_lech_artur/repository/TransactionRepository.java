package pk.la.pasir_lech_artur.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pk.la.pasir_lech_artur.model.Transaction;
import pk.la.pasir_lech_artur.model.User;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findAllByUser(User user);

    List<Transaction> findByUser(User user);

    List<Transaction> findAllByUserAndTimestampGreaterThanEqual(User user, LocalDateTime timestamp);

    List<Transaction> findAllByUserAndTimestampBetween(
            User user,
            LocalDateTime from,
            LocalDateTime to
    );

    List<Transaction> findAllByUserAndTimestampGreaterThanEqualAndTimestampLessThanEqual(
            User user,
            LocalDateTime from,
            LocalDateTime to
    );
}