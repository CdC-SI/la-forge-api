package ch.admin.zas.jweb.laforge.common.page;

import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Prédicats de pagination par clés (keyset), réutilisés par tous les domaines paginant sur une
 * clé de tri simple + {@code id} en départage. Stable face aux insertions concurrentes,
 * contrairement à une pagination par décalage ({@code OFFSET}).
 */
public final class KeysetPredicates {

    private KeysetPredicates() {
    }

    /** Page suivante pour un tri ascendant sur {@code keyAttribute}, puis {@code id} ascendant. */
    public static <T, K extends Comparable<? super K>> Specification<T> afterAscending(
            String keyAttribute, K keyValue, UUID idValue) {
        return (root, query, cb) -> {
            if (keyValue == null) {
                return cb.conjunction();
            }
            var key = root.<K>get(keyAttribute);
            var id = root.<UUID>get("id");
            return cb.or(
                    cb.greaterThan(key, keyValue),
                    cb.and(cb.equal(key, keyValue), cb.greaterThan(id, idValue)));
        };
    }

    /** Page suivante pour un tri descendant sur {@code keyAttribute}, puis {@code id} ascendant. */
    public static <T, K extends Comparable<? super K>> Specification<T> afterDescending(
            String keyAttribute, K keyValue, UUID idValue) {
        return (root, query, cb) -> {
            if (keyValue == null) {
                return cb.conjunction();
            }
            var key = root.<K>get(keyAttribute);
            var id = root.<UUID>get("id");
            return cb.or(
                    cb.lessThan(key, keyValue),
                    cb.and(cb.equal(key, keyValue), cb.greaterThan(id, idValue)));
        };
    }
}
