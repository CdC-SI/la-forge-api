package ch.admin.zas.jweb.laforge.security.config;

import ch.admin.zas.jweb.laforge.security.web.CurrentAccountArgumentResolver;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Enregistre les résolveurs d'argument personnalisés (compte courant issu du JWT). */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentAccountArgumentResolver currentAccountArgumentResolver;

    public WebMvcConfig(CurrentAccountArgumentResolver currentAccountArgumentResolver) {
        this.currentAccountArgumentResolver = currentAccountArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentAccountArgumentResolver);
    }
}
