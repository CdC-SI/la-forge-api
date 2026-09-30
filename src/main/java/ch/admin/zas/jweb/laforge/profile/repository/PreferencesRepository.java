package ch.admin.zas.jweb.laforge.profile.repository;

import ch.admin.zas.jweb.laforge.profile.domain.Preferences;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreferencesRepository extends JpaRepository<Preferences, UUID> {

    Optional<Preferences> findByAccount_Id(UUID accountId);
}
