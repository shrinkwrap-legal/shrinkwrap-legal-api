package legal.shrinkwrap.api.persistence.repo;

import legal.shrinkwrap.api.persistence.entity.CaseLawAnalysisEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CaseLawAnalysisRepository extends JpaRepository<CaseLawAnalysisEntity, Long> {
    // Find all analyses by analysisType and the ID of the related CaseLawEntity
    Optional<CaseLawAnalysisEntity> findFirstByAnalysisTypeAndCaseLaw_IdOrderByAnalysisVersionDesc(String analysisType, Long caseLawId);

    // Find all analyses by analysisType and sentence_hash not referencing another caselaw
    Optional<CaseLawAnalysisEntity> findFirstBySentenceHashAndIdenticalToIsNull(String sentenceHash);

    @Deprecated
    Page<CaseLawAnalysisEntity> findAllBySentenceHashIsNullAndAnalysisType(String analysisType, Pageable pageable);

    @Query(value = """
        SELECT *
        FROM caselaw_analysis
        WHERE search_vector @@ plainto_tsquery('german', :query)
        ORDER BY ts_rank(search_vector, plainto_tsquery('german', :query)) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<CaseLawAnalysisEntity> searchPostgresFullText(@Param("query") String query, @Param("limit") int limit);
}
