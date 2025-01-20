package legal.shrinkwrap.api.persistence.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import legal.shrinkwrap.api.persistence.entity.CaseLawMetadataEntity;

public interface CaseLawMetadataRepository extends JpaRepository<CaseLawMetadataEntity, Long> {
}
