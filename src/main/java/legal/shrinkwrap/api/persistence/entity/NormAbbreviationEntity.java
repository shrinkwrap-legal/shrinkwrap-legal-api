package legal.shrinkwrap.api.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * An abbreviation a law is known by. A child table rather than an array, because it has to be
 * indexable and because one abbreviation can point at several laws.
 * <p>
 * Aliases accumulate and are never dropped: decisions go on citing with the abbreviation of
 * their day for decades. RIS also leaves the field empty for laws that plainly have a common
 * short form - the Urheberrechtsgesetz carries no abbreviation although everyone writes UrhG -
 * which is why the short title is held here too.
 */
@Entity
@Table(name = "norm_abbreviation",
        uniqueConstraints = @UniqueConstraint(name = "norm_abbreviation_identity",
                columnNames = {"norm_id", "normalized"}),
        indexes = {
                @Index(name = "norm_abbreviation_normalized_index", columnList = "normalized"),
                @Index(name = "norm_abbreviation_norm_index", columnList = "norm_id")
        })
@Getter
@Setter
public class NormAbbreviationEntity {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "norm_id")
    private NormEntity norm;

    /**
     * As written, for display: "StVO 1960" - or the short title, which RIS lets run to a full
     * sentence: the longest in the corpus has 539 characters. Unbounded, because it is shown
     * rather than searched; the bound belongs on {@link #normalized}, which is the index key.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String abkuerzung;

    /**
     * The lookup key. One abbreviation yields several rows, because a query has to hit whichever
     * form the caller used: "StVO 1960" produces "stvo 1960", "stvo" and "stvo1960". The query
     * is normalised the same way.
     */
    @Column(nullable = false, length = 256)
    private String normalized;

    /**
     * Holds a {@link NormAbbreviationSource} name. Kept as a string for the same reason
     * {@code CaseLawEntity.court} is: mapping the enum itself makes Hibernate add a check
     * constraint listing today's values, and ddl-auto never updates one afterwards - so the
     * constraint would silently drift apart from the enum, and the first row carrying a new
     * value would fail in production. Section 8 of the norm concept expects new values.
     */
    @Column(nullable = false, length = 32)
    private String quelle;

    @CreationTimestamp
    @Column(name = "first_seen")
    private LocalDateTime firstSeen;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;
}
