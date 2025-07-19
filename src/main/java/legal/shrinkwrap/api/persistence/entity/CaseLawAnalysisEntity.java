package legal.shrinkwrap.api.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "caselaw_analysis", indexes = {
        @Index(name = "analysis_type_index", columnList = "analysis_type"),
        @Index(name="case_law_id", columnList = "case_law_id"),
        @Index(name="sentence_hash_index", columnList = "sentence_hash")
})
@Getter
@Setter
public class CaseLawAnalysisEntity {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private CaseLawEntity caseLaw;

    @Column(name = "analysis_type")
    private String analysisType = "text";

    @Column(name = "analysis_subtype")
    private String analysisSubType;

    @Column(name = "analysis_version")
    private Integer analysisVersion = 1;

    @CreationTimestamp
    private LocalDateTime created;

    @UpdateTimestamp
    private LocalDateTime updated;

    @Column(name="full_text", columnDefinition = "TEXT")
    private String fullText;

    @Column(name="analysis", columnDefinition = "JSON")
    @ColumnTransformer(write = "?::json")
    private String analysis;

    @Column
    @Lob
    private String sentences;

    @Column(name = "word_count")
    private Long wordCount;

    @Column(name = "sentence_hash", length = 65000)
    private String sentenceHash;

    @ManyToOne(fetch = FetchType.LAZY)
    private CaseLawEntity identicalTo;
}
