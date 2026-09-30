package ch.admin.zas.jweb.laforge.security.web;

import ch.admin.zas.jweb.laforge.common.error.UnauthenticatedException;
import ch.admin.zas.jweb.laforge.security.dto.CurrentAccountDto;
import ch.admin.zas.jweb.laforge.security.domain.Role;
import ch.admin.zas.jweb.laforge.security.repository.AccountRepository;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.UUID;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Résout les paramètres {@code @CurrentAccount} en chargeant le compte identifié par le claim
 * {@code sub} du JWT authentifié. Les rôles effectifs proviennent exclusivement de ses autorités,
 * jamais des attributions actuelles en base. Échoue si aucune authentification JWT n'est présente : les
 * routes utilisant cette annotation doivent être protégées par {@code SecurityConfig}.
 */
@Component
public class CurrentAccountArgumentResolver implements HandlerMethodArgumentResolver {

    private final AccountRepository accountRepository;

    public CurrentAccountArgumentResolver(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentAccount.class)
                && parameter.getParameterType() == CurrentAccountDto.class;
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication != null && authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new UnauthenticatedException("Authentification requise.");
        }
        var accountId = UUID.fromString(jwt.getSubject());
        var effectiveRoles = Arrays.stream(Role.values())
                .filter(role -> authentication.getAuthorities().stream()
                        .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role.name())))
                .collect(Collectors.toSet());
        return accountRepository.findById(accountId)
                .map(account -> new CurrentAccountDto(account.getId(), account.getEmail(), account.getDisplayName(),
                        account.getStatus(), effectiveRoles))
                .orElseThrow(() -> new UnauthenticatedException("Le compte associé au jeton n'existe plus."));
    }
}
