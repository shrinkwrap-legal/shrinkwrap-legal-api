package legal.shrinkwrap.api.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One version of one provision - a row per RIS document.
 * <p>
 * There is deliberately no entity between {@link NormEntity} and this one. The ASVG has 24
 * simultaneously valid "Art. 2", so a provision identity could not be formed unambiguously;
 * "§ 16 ABGB as of X" is a <em>query</em> instead, and if it returns several rows that is the
 * correct answer - the citation really is ambiguous.
 */
@Entity
@Table(name = "norm_document",
        indexes = {
                @Index(name = "norm_document_norm_index", columnList = "norm_id"),
                @Index(name = "norm_document_range_index", columnList = "norm_id, nummer, buchstabe"),
                @Index(name = "norm_document_sort_index", columnList = "norm_id, sort_index"),
                @Index(name = "norm_document_eli_index", columnList = "eli_provision")
        })
@Getter
@Setter
public class NormDocumentEntity {

    @Id
    @GeneratedValue
    private Long id;

    @CreationTimestamp
    private LocalDateTime created;

    @UpdateTimestamp
    private LocalDateTime updated;

    @Column(name = "docnumber", nullable = false, unique = true, length = 64)
    private String docNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "norm_id")
    private NormEntity norm;

    /** Kept for EUR-Lex, where the language is part of the document identity. */
    @Column(nullable = false, length = 8)
    private String language = "de";

    /**
     * Position in the law, taken from the order RIS returns. The RIS sort parameters are
     * unusable - they sort "§ 10" before "§ 2" - so only the default order of the result list
     * is, and it is persisted here.
     */
    @Column(name = "sort_index")
    private Integer sortIndex;

    /**
     * ELI path without the trailing document number ({@code /NOR…}, {@code /LOO…}) - the stable
     * key of a provision across its versions. Only the path is stored, the host varies between
     * ris.bka.gv.at, www.ris.bka.gv.at and ogd.ris.bka.gv.at.
     */
    @Column(name = "eli_provision", length = 512)
    private String eliProvision;

    /**
     * Holds a {@link NormSectionType} name. Kept as a string for the same reason
     * {@code CaseLawEntity.court} is: mapping the enum itself makes Hibernate add a check
     * constraint listing today's values, and ddl-auto never updates one afterwards - so the
     * constraint would silently drift apart from the enum, and the first row carrying a new
     * value would fail in production. Section 8 of the norm concept expects new values.
     */
    @Column(name = "abschnitt_typ", length = 32)
    private String abschnittTyp;

    /** Full designation as RIS writes it: "§ 50", "Art. 2", "Anl. 2/49". */
    @Column(name = "artikel_paragraph_anlage", length = 256)
    private String artikelParagraphAnlage;

    @Column(length = 32)
    private String nummer;

    @Column(length = 16)
    private String buchstabe;

    /** Carries the distinction on its own where a law numbers annexes "Anl. 2/01" … "2/49". */
    @Column(length = 32)
    private String teil;

    /**
     * "ÜR" marks transitional law - typically the last articles of a law, carrying the amending
     * act as their Stammnorm rather than the law's own. RIS uses the same field for other markers
     * too ("EG/EU", "A", "S", "BVG"), so it is stored as written.
     */
    @Column(length = 256)
    private String uebergangsrecht;

    @Column(columnDefinition = "DATE")
    private LocalDate inkrafttreten;

    /** Frequently absent - a provision in force has no end date. */
    @Column(columnDefinition = "DATE")
    private LocalDate ausserkrafttreten;

    @Column(name = "stammnorm_publikationsorgan", length = 256)
    private String stammnormPublikationsorgan;

    @Column(name = "stammnorm_bgblnummer", length = 128)
    private String stammnormBgblnummer;

    @Column(name = "novellen_publikationsorgan", length = 256)
    private String novellenPublikationsorgan;

    @Column(name = "novellen_bgblnummer", length = 128)
    private String novellenBgblnummer;

    @Column(name = "novellen_beziehung", length = 256)
    private String novellenBeziehung;

    @Column(columnDefinition = "TEXT")
    private String beachte;

    @Column(columnDefinition = "TEXT")
    private String anmerkung;

    @Column(name = "geaendert", columnDefinition = "DATE")
    private LocalDate geaendert;

    @Column(name = "html_url", length = 1024)
    private String htmlUrl;

    /**
     * The cleaned html, and the representation that carries the structure - image sources in it
     * are rewritten to absolute, so it stands on its own.
     */
    @Column(name = "html", columnDefinition = "TEXT")
    @Basic(fetch = FetchType.LAZY)
    private String fullCleanHtml;

    @Column(name = "full_text", columnDefinition = "TEXT")
    @Basic(fetch = FetchType.LAZY)
    private String fullText;

    @Column(name = "word_count")
    private Long wordCount;

    /**
     * Which version of the html preprocessing produced {@link #fullText}. When the conversion is
     * corrected - and it will be - rows with an older version can be converted again on purpose.
     */
    @Column(name = "text_conversion_version")
    private Integer textConversionVersion;

    /** Images and annex files as delivered by RIS; see the norm concept document, section 7.3. */
    @Column(name = "attachments", columnDefinition = "JSON")
    @ColumnTransformer(write = "?::json")
    private String attachments;

    /** Soft delete - a row RIS has withdrawn stays, so old citations keep resolving. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "metadata", columnDefinition = "JSON")
    @ColumnTransformer(write = "?::json")
    private String metadata;
}
