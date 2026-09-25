package legal.shrinkwrap.api.service;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Normalisation of the names a law can be looked up by - its abbreviation and its short title.
 * Used by the import when storing them and by the search when resolving a query, which is the
 * only reason this is a class of its own: both sides have to run the same rules.
 * <p>
 * One name yields several keys, because a caller writes whichever form comes to mind:
 * "StVO 1960" has to be found by "StVO", by "StVO 1960" and by "stvo1960".
 */
public final class NormAbbreviations {

    /** A year appended to the short title - "StVO 1960", "EStG 1988". */
    private static final String TRAILING_YEAR = "\\s+[12]\\d{3}$";

    /** Invisible, so it stays an escape; every other character in this class is written as is. */
    private static final char NON_BREAKING_SPACE = '\u00a0';

    private NormAbbreviations() {
    }

    /**
     * @return every lookup key an abbreviation should be findable under, in a stable order;
     *         empty if there is nothing to normalise
     */
    public static Set<String> normalize(String abbreviation) {
        if (abbreviation == null || abbreviation.isBlank()) {
            return Set.of();
        }
        String base = collapseWhitespace(abbreviation).toLowerCase(Locale.ROOT);
        String withoutYear = base.replaceAll(TRAILING_YEAR, "").trim();

        Set<String> keys = new LinkedHashSet<>();
        keys.add(base);
        keys.add(condense(base));
        keys.add(withoutYear);
        keys.add(condense(withoutYear));
        keys.removeIf(String::isBlank);
        return keys;
    }

    /**
     * Only the keys of the name as written, without dropping its year: "TKG 2003" yields
     * "tkg 2003" and "tkg2003", but not "tkg". A query tries these first - the year is how a
     * caller tells TKG 2003 from TKG 2021, and stripping it would make the two alike.
     */
    public static Set<String> asWritten(String abbreviation) {
        if (abbreviation == null || abbreviation.isBlank()) {
            return Set.of();
        }
        String base = collapseWhitespace(abbreviation).toLowerCase(Locale.ROOT);
        Set<String> keys = new LinkedHashSet<>();
        keys.add(base);
        keys.add(condense(base));
        return keys;
    }

    /**
     * RIS separates a short title from its year with a non-breaking space, as in
     * {@code "Buchpreisbindungsgesetz\u00a02023"}. Java's {@code \s} does not match that
     * character, so without replacing it first the year would never be stripped and the law
     * would not be findable under "buchpreisbindungsgesetz".
     */
    private static String collapseWhitespace(String value) {
        return value.replace(NON_BREAKING_SPACE, ' ').trim().replaceAll("\\s+", " ");
    }

    /**
     * Without dots and whitespace: "StVO 1960" becomes "stvo1960".
     * <p>
     * Hyphens deliberately stay. They carry meaning in a citation - "B-VG" is the correct form
     * and "BVG" is a different thing, just as "Bundes-Verfassungsgesetz" is not
     * "Bundesverfassungsgesetz". Dropping them would merge names that are genuinely distinct.
     */
    private static String condense(String value) {
        return value.replaceAll("[.\\s]", "");
    }
}
