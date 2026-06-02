package pk.la.pasir_lech_artur.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pk.la.pasir_lech_artur.model.Membership;

import java.util.List;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    List<Membership> findByGroupId(Long groupId);

    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    void deleteByGroupId(Long groupId);
}
