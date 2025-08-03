package legal.shrinkwrap.api.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "common_sentences")
public class CommonSentences {
    @Id
    @GeneratedValue
    private Long id;

    @CreationTimestamp
    private LocalDateTime created;

    @UpdateTimestamp
    private LocalDateTime updated;

    @ManyToOne(fetch = FetchType.LAZY)
    private CaseLawEntity caseLaw;

    private String applicationType; //Justiz, DSB, etc

    @Column(name="sentence_hash", length = 10000, unique = true)
    private String sentenceHash;

    @Column(name="full_text", columnDefinition = "TEXT")
    private String fullText;
}
