package ch.admin.zas.jweb.laforge.common.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration transverse de l'application : horloge unique en UTC (toutes les échéances et
 * horodatages du contrat sont calculés à partir de cette horloge, jamais de {@code new Date()}
 * ou {@code Instant.now()} directs), activation des propriétés {@link LaForgeProperties}, de
 * l'exécution asynchrone (envoi des courriels après commit, hors du thread de la requête) et de
 * l'ordonnancement (purge des enregistrements d'idempotence expirés).
 */
@Configuration
@EnableConfigurationProperties(LaForgeProperties.class)
@EnableAsync
@EnableScheduling
public class ApplicationConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
