package pk.la.pasir_lech_artur.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pk.la.pasir_lech_artur.model.Debt;

import java.util.List;

public interface DebtRepository extends JpaRepository<Debt, Long> {

    List<Debt> findByGroupId(Long groupId);

    void deleteByGroupId(Long groupId);
}
