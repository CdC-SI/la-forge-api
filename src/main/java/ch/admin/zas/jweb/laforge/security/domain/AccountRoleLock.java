package ch.admin.zas.jweb.laforge.security.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Ligne singleton créée par migration pour sérialiser les changements de rôles. */
@Entity
@Table(name = "account_role_lock")
public class AccountRoleLock {

    @Id
    private Integer id;

    protected AccountRoleLock() {
    }
}
