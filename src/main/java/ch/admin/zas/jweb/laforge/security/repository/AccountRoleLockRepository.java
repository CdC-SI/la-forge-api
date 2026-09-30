package ch.admin.zas.jweb.laforge.security.repository;

import ch.admin.zas.jweb.laforge.security.domain.AccountRoleLock;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface AccountRoleLockRepository extends JpaRepository<AccountRoleLock, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from AccountRoleLock l where l.id = 1")
    Optional<AccountRoleLock> lockRoleChanges();
}
