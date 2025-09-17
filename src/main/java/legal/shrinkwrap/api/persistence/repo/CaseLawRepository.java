package legal.shrinkwrap.api.persistence.repo;

import legal.shrinkwrap.api.persistence.entity.CaseLawEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;


public interface CaseLawRepository  extends JpaRepository<CaseLawEntity, Long> {
    Optional<CaseLawEntity> findCaseLawEntityByEcli(String ecli);

    Optional<CaseLawEntity> findCaseLawEntityByDocNumber(String docNumber);

    @Query("SELECT c FROM CaseLawEntity c WHERE NOT EXISTS (SELECT a FROM CaseLawAnalysisEntity a WHERE a.caseLaw = c)")
    Page<CaseLawEntity> findCaseLawWithoutAnalysis(Pageable pageable);
}
