package ch.admin.zas.jweb.laforge.security.repository;

import ch.admin.zas.jweb.laforge.security.domain.Account;
import ch.admin.zas.jweb.laforge.security.domain.AccountStatus;
import ch.admin.zas.jweb.laforge.security.domain.Role;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
            select a from Account a
            where (lower(a.email) like :pattern escape '!' or lower(a.displayName) like :pattern escape '!')
              and (:afterId is null or a.displayName > :afterName
                   or (a.displayName = :afterName and a.id > :afterId))
            order by a.displayName, a.id
            """)
    List<Account> search(String pattern, String afterName, UUID afterId, Pageable pageable);

    @Query("select count(a) from Account a where a.status = :status and :role member of a.roles")
    long countByStatusAndRole(AccountStatus status, Role role);
}
