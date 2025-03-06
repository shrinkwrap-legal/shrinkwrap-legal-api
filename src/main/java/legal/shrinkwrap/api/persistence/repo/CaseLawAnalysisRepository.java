package legal.shrinkwrap.api.persistence.repo;

import legal.shrinkwrap.api.persistence.entity.CaseLawAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaseLawAnalysisRepository extends JpaRepository<CaseLawAnalysisEntity, Long> {
}
