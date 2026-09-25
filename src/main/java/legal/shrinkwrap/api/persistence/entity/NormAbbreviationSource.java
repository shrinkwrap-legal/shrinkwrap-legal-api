package legal.shrinkwrap.api.persistence.entity;

/** Where a lookup name was picked up. Names accumulate and are never dropped. */
public enum NormAbbreviationSource {
    /** The Abkuerzung field of the RIS document. */
    BRKONS,
    /**
     * The short title. Held alongside the abbreviations so one lookup serves both - RIS leaves
     * the abbreviation empty often enough that a law would otherwise have no key at all, and a
     * caller is as likely to write "Buchpreisbindungsgesetz" as "BPrBG".
     */
    KURZTITEL
}
