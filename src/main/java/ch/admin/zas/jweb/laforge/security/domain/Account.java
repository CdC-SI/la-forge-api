package ch.admin.zas.jweb.laforge.security.domain;

import ch.admin.zas.jweb.laforge.common.error.ForbiddenException;
import ch.admin.zas.jweb.laforge.common.error.InvalidStateException;
import ch.admin.zas.jweb.laforge.common.persistence.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Compte utilisateur : identité, secret d'authentification et rôles cumulatifs. Les préférences
 * de pratique (schéma {@code Preferences}) sont portées par le domaine {@code profile}, pas ici,
 * pour garder la sécurité indépendante des préférences métier.
 */
@Entity
@Table(name = "account", uniqueConstraints = @UniqueConstraint(name = "uk_account_email", columnNames = "email"))
public class Account extends BaseEntity {

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AccountStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "account_role", joinColumns = @JoinColumn(name = "account_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Set<Role> roles = new LinkedHashSet<>();

    protected Account() {
        // Requis par JPA.
    }

    public Account(String email, String passwordHash, String displayName) {
        this.email = email.strip().toLowerCase();
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.status = AccountStatus.PENDING_VERIFICATION;
    }

    /** Active le compte suite à la confirmation du jeton de vérification et attribue LEARNER. */
    public void activate() {
        if (status != AccountStatus.PENDING_VERIFICATION) {
            throw new InvalidStateException("Le compte n'est pas en attente d'activation.");
        }
        status = AccountStatus.ACTIVE;
        roles.add(Role.LEARNER);
    }

    /** Vérifie que le compte peut s'authentifier (statut ACTIVE uniquement). */
    public void assertCanAuthenticate() {
        switch (status) {
            case ACTIVE -> {
                // rien à faire, authentification autorisée
            }
            case PENDING_VERIFICATION -> throw new ForbiddenException("Le compte n'est pas encore activé.");
            case LOCKED, DISABLED -> throw new ForbiddenException("Le compte ne peut plus s'authentifier.");
        }
    }

    public boolean hasAnyRole(Set<Role> requiredRoles) {
        return !java.util.Collections.disjoint(roles, requiredRoles);
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void rename(String newDisplayName) {
        this.displayName = newDisplayName;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public Set<Role> getRoles() {
        return Set.copyOf(roles);
    }
}
