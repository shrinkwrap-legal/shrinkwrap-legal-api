package legal.shrinkwrap.api.persistence.repo;

import legal.shrinkwrap.api.persistence.entity.CommonSentences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CommonSentencesRepository extends JpaRepository<CommonSentences, Long> {

    @Query("SELECT s.sentenceHash FROM CommonSentences s")
    List<String> findAllSentenceHash();

    Optional<CommonSentences> findFirstBySentenceHash(String sentenceHash);
}
