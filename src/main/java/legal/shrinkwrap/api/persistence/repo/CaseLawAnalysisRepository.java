package legal.shrinkwrap.api.persistence.repo;

import legal.shrinkwrap.api.persistence.entity.CaseLawAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CaseLawAnalysisRepository extends JpaRepository<CaseLawAnalysisEntity, Long> {
    // Find all analyses by analysisType and the ID of the related CaseLawEntity
    Optional<CaseLawAnalysisEntity> findFirstByAnalysisTypeAndCaseLaw_IdOrderByAnalysisVersionDesc(String analysisType, Long caseLawId);

    // Find all analyses by analysisType and sentence_hash not referencing another caselaw
    Optional<CaseLawAnalysisEntity> findFirstBySentenceHashAndIdenticalToIsNull(String sentenceHash);
}
