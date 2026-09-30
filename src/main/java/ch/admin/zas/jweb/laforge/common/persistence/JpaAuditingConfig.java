package ch.admin.zas.jweb.laforge.common.persistence;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Active l'audit JPA ({@code @CreatedDate} / {@code @LastModifiedDate}) en s'appuyant sur
 * l'horloge applicative unique plutôt que sur l'horloge système, afin que tous les horodatages
 * restent cohérents et testables (voir {@code ApplicationConfig#clock()}).
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "offsetDateTimeProvider")
public class JpaAuditingConfig {

    @Bean
    public DateTimeProvider offsetDateTimeProvider(Clock clock) {
        return () -> Optional.of(OffsetDateTime.now(clock));
    }
}
