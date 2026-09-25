package legal.shrinkwrap.api.persistence.repo;

import legal.shrinkwrap.api.persistence.entity.NormAbbreviationEntity;
import legal.shrinkwrap.api.persistence.entity.NormEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NormAbbreviationRepository extends JpaRepository<NormAbbreviationEntity, Long> {

    /**
     * Several laws can share a normalised abbreviation, so this returns a list. Handing the
     * ambiguity back to the caller is the correct answer; picking one silently is not.
     */
    List<NormAbbreviationEntity> findByNormalized(String normalized);

    Optional<NormAbbreviationEntity> findByNormAndNormalized(NormEntity norm, String normalized);

    /**
     * One query input yields several normalised keys, any of which may be the stored one, so the
     * ids have to be deduplicated.
     * <p>
     * Ids rather than whole laws on purpose: {@code SELECT DISTINCT} over an entity selects
     * every one of its columns, and PostgreSQL has no equality operator for {@code json} - the
     * metadata column would make the query fail with SQLSTATE 42883. H2 permits it, so the
     * difference only shows against the real database.
     */
    @Query("SELECT DISTINCT a.norm.id FROM NormAbbreviationEntity a "
            + "WHERE a.normalized IN :keys AND a.norm.jurisdiction = :jurisdiction")
    List<Long> findNormIdsByNormalizedIn(Collection<String> keys, String jurisdiction);

    List<NormAbbreviationEntity> findByNorm(NormEntity norm);
}
