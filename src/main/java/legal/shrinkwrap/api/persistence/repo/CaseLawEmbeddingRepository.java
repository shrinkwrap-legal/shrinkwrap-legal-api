package legal.shrinkwrap.api.persistence.repo;

import legal.shrinkwrap.api.persistence.entity.CaseLawEmbeddingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CaseLawEmbeddingRepository extends JpaRepository<CaseLawEmbeddingEntity, Long> {

    Optional<CaseLawEmbeddingEntity> findFirstByCaseLaw_Id(Long caseLawId);
}
