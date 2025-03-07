package legal.shrinkwrap.api.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "caselaw")
@Getter
@Setter
@ToString
public class CaseLawEntity {
    @Id
    @GeneratedValue
    private Long id;

    @CreationTimestamp
    private LocalDateTime created;

    @UpdateTimestamp
    private LocalDateTime updated;

    @Column(name = "ecli", nullable = true)
    private String ecli;

    @Column(name = "docnumber", nullable = false, unique = true)
    private String docNumber;

    @Column(length = 2048)
    private String caseNumber;

    private String applicationType; //Justiz, DSB, etc

    private String organ; //organ

    private String court;

    private String decisionType; //VfGH, VwGH

    private String url;

    private String htmlUrl;

    @Column(name = "decision_date", columnDefinition = "DATE")
    private LocalDate decisionDate;

    @Column(name = "published", columnDefinition = "DATE")
    private LocalDate publishedDate;

    @Column(name = "last_changed", columnDefinition = "DATE")
    private LocalDate lastChangedDate;

    @Column(name = "metadata", columnDefinition = "JSON")
    @ColumnTransformer(write = "?::json")
    private String metadata;

    @Column(name = "html", columnDefinition = "TEXT")
    @Basic(fetch = FetchType.LAZY)
    private String fullCleanHtml;

}
