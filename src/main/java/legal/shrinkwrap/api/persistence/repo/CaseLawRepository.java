package legal.shrinkwrap.api.persistence.repo;

import legal.shrinkwrap.api.persistence.entity.CaseLawEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface CaseLawRepository  extends JpaRepository<CaseLawEntity, Long> {
    Optional<CaseLawEntity> findCaseLawEntityByEcli(String ecli);

    Optional<CaseLawEntity> findCaseLawEntityByDocNumber(String docNumber);
}
