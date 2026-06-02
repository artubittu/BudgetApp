package pk.la.pasir_lech_artur.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pk.la.pasir_lech_artur.model.Group;
import pk.la.pasir_lech_artur.model.User;

import java.util.List;

public interface GroupRepository extends JpaRepository<Group, Long> {

    List<Group> findByMemberships_User(User user);
}
