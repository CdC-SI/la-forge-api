package ch.admin.zas.jweb.laforge.security.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hibernate.validator.constraints.UniqueElements;

/** Remplacement complet des rôles ; les valeurs inconnues sont des violations métier (422). */
public record ReplaceAccountRolesInput(
        @NotEmpty @Size(max = 3) @UniqueElements List<@NotNull @Pattern(regexp = "LEARNER|AUTHOR|ADMIN") String> roles) {
    public ReplaceAccountRolesInput {
        roles = roles == null ? null : Collections.unmodifiableList(new ArrayList<>(roles));
    }
}
