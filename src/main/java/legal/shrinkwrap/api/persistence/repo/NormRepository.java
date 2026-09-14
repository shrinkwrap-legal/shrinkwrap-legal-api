package legal.shrinkwrap.api.persistence.repo;

import legal.shrinkwrap.api.persistence.entity.NormEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface NormRepository extends JpaRepository<NormEntity, Long> {

    Optional<NormEntity> findByJurisdictionAndSourceAndIdentifier(String jurisdiction,
                                                                  String source,
                                                                  String identifier);

    /** The API addresses a law by jurisdiction and law number; the source follows from those. */
    Optional<NormEntity> findByJurisdictionAndIdentifier(String jurisdiction, String identifier);

    /**
     * Substring match on the titles, for when the exact name is not what the caller wrote -
     * "Wohnrechtsänderungsgesetz" has to reach the "2. Wohnrechtsänderungsgesetz". This is what
     * the pg_trgm indexes of 2026-09-04_norms.sql are for; a btree cannot serve a leading
     * wildcard.
     */
    @Query("SELECT n FROM NormEntity n WHERE n.jurisdiction = :jurisdiction "
            + "AND (LOWER(n.kurztitel) LIKE :pattern OR LOWER(n.langtitel) LIKE :pattern) "
            + "ORDER BY LENGTH(n.kurztitel)")
    List<NormEntity> findByTitleLike(String jurisdiction, String pattern, Limit limit);
}
