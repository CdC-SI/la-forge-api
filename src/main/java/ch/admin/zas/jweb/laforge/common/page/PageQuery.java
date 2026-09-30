package ch.admin.zas.jweb.laforge.common.page;

/**
 * Paramètres de pagination normalisés à partir des paramètres de requête {@code limit} et
 * {@code cursor} définis par le contrat (composants {@code Limit} et {@code Cursor}).
 */
public record PageQuery(int limit, String cursor) {

    public static final int DEFAULT_LIMIT = 20;
    public static final int MAX_LIMIT = 100;

    /** Clamp systématique de la limite : une valeur absente ou hors bornes retombe sur les bornes du contrat. */
    public PageQuery {
        if (limit <= 0) {
            limit = DEFAULT_LIMIT;
        } else if (limit > MAX_LIMIT) {
            limit = MAX_LIMIT;
        }
    }

    public static PageQuery of(Integer limit, String cursor) {
        return new PageQuery(limit == null ? DEFAULT_LIMIT : limit, cursor);
    }

    public boolean hasCursor() {
        return cursor != null && !cursor.isBlank();
    }
}
