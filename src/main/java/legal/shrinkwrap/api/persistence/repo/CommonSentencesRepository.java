package legal.shrinkwrap.api.persistence.repo;

import legal.shrinkwrap.api.persistence.entity.CaseLawEntity;
import legal.shrinkwrap.api.persistence.entity.CommonSentences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommonSentencesRepository extends JpaRepository<CommonSentences, Long> {

    List<String> findAllSentenceHash();
}
