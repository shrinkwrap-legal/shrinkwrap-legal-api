package legal.shrinkwrap.api.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "caselaw_analysis")
@Getter
@Setter
public class CaseLawAnalysisEntity {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private CaseLawEntity caseLaw;

    @Column(name = "analysis_type")
    private String analysisType = "fullAnalysis";


    @CreationTimestamp
    private LocalDateTime created;

    @UpdateTimestamp
    private LocalDateTime updated;

    @Column
    @Lob
    private String fullText;

    @Column
    @Lob
    private String sentences;

    @Column(name = "word_count")
    private Long wordCount;
}
