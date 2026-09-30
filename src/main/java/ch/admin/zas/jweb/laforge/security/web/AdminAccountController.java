package ch.admin.zas.jweb.laforge.security.web;

import ch.admin.zas.jweb.laforge.common.page.Page;
import ch.admin.zas.jweb.laforge.common.page.PageQuery;
import ch.admin.zas.jweb.laforge.security.dto.AdminAccountDto;
import ch.admin.zas.jweb.laforge.security.dto.CurrentAccountDto;
import ch.admin.zas.jweb.laforge.security.dto.ReplaceAccountRolesInput;
import ch.admin.zas.jweb.laforge.security.service.AdminAccountService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Recherche administrative et remplacement des rôles des autres comptes actifs. */
@RestController
@RequestMapping("/admin/accounts")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAccountController {
    private final AdminAccountService service;

    public AdminAccountController(AdminAccountService service) {
        this.service = service;
    }

    @GetMapping
    public Page<AdminAccountDto> search(@CurrentAccount CurrentAccountDto actor,
            @RequestParam(required = false) String query, @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor) {
        return service.search(actor, query, PageQuery.of(limit, cursor));
    }

    @PutMapping("/{accountId}/roles")
    public AdminAccountDto replaceRoles(@CurrentAccount CurrentAccountDto actor, @PathVariable UUID accountId,
            @Valid @RequestBody ReplaceAccountRolesInput input) {
        return service.replaceRoles(actor, accountId, input);
    }
}
