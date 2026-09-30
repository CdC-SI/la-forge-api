package ch.admin.zas.jweb.laforge.common.page;

import java.util.List;

/**
 * Page de résultats paginée par curseur opaque, conforme aux schémas {@code *Page} du contrat.
 * {@code nextCursor} est absent (null) à la dernière page.
 */
public record Page<T>(List<T> items, String nextCursor) {

    public Page {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static <T> Page<T> of(List<T> items, String nextCursor) {
        return new Page<>(items, nextCursor);
    }

    public static <T> Page<T> last(List<T> items) {
        return new Page<>(items, null);
    }
}
