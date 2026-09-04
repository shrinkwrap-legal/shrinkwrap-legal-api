package legal.shrinkwrap.api.persistence.repo;

import legal.shrinkwrap.api.persistence.entity.CaseLawAnalysisEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CaseLawAnalysisRepository extends JpaRepository<CaseLawAnalysisEntity, Long> {
    // Find all analyses by analysisType and the ID of the related CaseLawEntity
    Optional<CaseLawAnalysisEntity> findFirstByAnalysisTypeAndCaseLaw_IdOrderByAnalysisVersionDesc(String analysisType, Long caseLawId);

    // Find all analyses by analysisType and sentence_hash not referencing another caselaw
    Optional<CaseLawAnalysisEntity> findFirstBySentenceHashAndIdenticalToIsNull(String sentenceHash);

    @Deprecated
    Page<CaseLawAnalysisEntity> findAllBySentenceHashIsNullAndAnalysisType(String analysisType, Pageable pageable);

    /**
     * Returns a List and not a Page on purpose: for a full page Spring Data runs a second, equally
     * expensive query only to count all matches - and the total is not used anywhere.
     */
    @Query(value = """
        SELECT ca.* 
        FROM caselaw_analysis ca 
        INNER JOIN caselaw c ON ca.case_law_id = c.id 
        WHERE ca.search_vector @@ plainto_tsquery('german', :query) 
                AND (CAST(:applicationType AS text) IS NULL OR c.application_type = :applicationType) 
                AND (CAST(:dateFrom AS date) IS NULL OR c.decision_date >= :dateFrom)
                AND (CAST(:dateTo AS date) IS NULL OR c.decision_date <= :dateTo)
        ORDER BY ts_rank(ca.search_vector, plainto_tsquery('german', :query)) DESC 
        """, nativeQuery = true)
    List<CaseLawAnalysisEntity> searchPostgresFullText(@Param("query") String query, @Param("applicationType") String applicationType, @Param("dateFrom") LocalDate dateFrom, @Param("dateTo") LocalDate dateTo, Pageable pageable);
}
