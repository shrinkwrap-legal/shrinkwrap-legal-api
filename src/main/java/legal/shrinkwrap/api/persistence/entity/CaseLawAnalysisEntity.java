package legal.shrinkwrap.api.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Table(name = "caselaw_analysis")
public class CaseLawAnalysisEntity {
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

    @Column(name = "wordcount")
    private Long wordCount;
}
