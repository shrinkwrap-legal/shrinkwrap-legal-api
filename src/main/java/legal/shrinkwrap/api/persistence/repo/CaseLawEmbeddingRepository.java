package legal.shrinkwrap.api.persistence.repo;

import legal.shrinkwrap.api.persistence.entity.CaseLawEmbeddingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaseLawEmbeddingRepository extends JpaRepository<CaseLawEmbeddingEntity, Long> {
}
