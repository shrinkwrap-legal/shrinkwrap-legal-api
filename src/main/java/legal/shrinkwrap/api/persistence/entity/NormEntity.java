package legal.shrinkwrap.api.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A body of law - the ABGB, the StVO - as opposed to a single provision of it, which is a
 * {@link NormDocumentEntity}.
 * <p>
 * The natural key is deliberately not RIS vocabulary: {@code (jurisdiction, source, identifier)}
 * carries the Gesetzesnummer today and can carry a CELEX number later without touching a query.
 */
@Entity
@Table(name = "norm",
        uniqueConstraints = @UniqueConstraint(name = "norm_identity",
                columnNames = {"jurisdiction", "source", "identifier"}),
        indexes = {@Index(name = "norm_identifier_index", columnList = "identifier")})
@Getter
@Setter
@ToString
public class NormEntity {

    @Id
    @GeneratedValue
    private Long id;

    @CreationTimestamp
    private LocalDateTime created;

    @UpdateTimestamp
    private LocalDateTime updated;

    /** ISO 3166-2: "AT" for federal law, "AT-9" for Vienna. */
    @Column(nullable = false, length = 8)
    private String jurisdiction;

    /**
     * Holds a {@link NormSource} name. Kept as a string for the same reason
     * {@code CaseLawEntity.court} is: mapping the enum itself makes Hibernate add a check
     * constraint listing today's values, and ddl-auto never updates one afterwards - so the
     * constraint would silently drift apart from the enum, and the first row carrying a new
     * value would fail in production. Section 8 of the norm concept expects new values.
     */
    @Column(nullable = false, length = 32)
    private String source;

    /** The RIS Gesetzesnummer; later a CELEX number. */
    @Column(nullable = false, length = 64)
    private String identifier;

    @Column(columnDefinition = "TEXT")
    private String kurztitel;

    /** The full official title; RIS calls this field "Titel". */
    @Column(columnDefinition = "TEXT")
    private String langtitel;

    /**
     * How RIS classifies the instrument. Short for the ordinary case - "BG", "V", "SV" - but
     * prose for treaties: "Vertrag – Europ. Agentur f. d. Betriebsmanagement von IT-Großsystemen"
     * runs to 69 characters. Unbounded, because there is no length RIS promises to stay under.
     */
    @Column(columnDefinition = "TEXT")
    private String typ;

    @Column(length = 512)
    private String eli;

    /** Several index entries joined together, so there is no useful bound. */
    @Column(columnDefinition = "TEXT")
    private String indizes;

    @Column(length = 512)
    private String kundmachungsorgan;

    @Column(name = "gesamte_rechtsvorschrift_url", length = 1024)
    private String gesamteRechtsvorschriftUrl;

    /**
     * Title and abbreviation are the current values, without history, and they do change while
     * the Gesetzesnummer stays put. Historical versions still carry the wording of their day, so
     * a plain "last write wins" during import would cement whichever document happened to be
     * processed last. This column records which Inkrafttretensdatum the current values came
     * from, and only a document at least as new is allowed to overwrite them.
     */
    @Column(name = "title_source_inkrafttreten", columnDefinition = "DATE")
    private LocalDate titleSourceInkrafttreten;

    @Column(name = "metadata", columnDefinition = "JSON")
    @ColumnTransformer(write = "?::json")
    private String metadata;

    /** Up to which as-of date the list of provisions is known to be complete. */
    @Column(name = "structure_loaded_for", columnDefinition = "DATE")
    private LocalDate structureLoadedFor;

    /** Last time the change feed was consulted for this law. */
    @Column(name = "last_change_check")
    private LocalDateTime lastChangeCheck;
}
