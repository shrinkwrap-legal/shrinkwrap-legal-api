package legal.shrinkwrap.api.persistence.repo;

import legal.shrinkwrap.api.persistence.entity.NormDocumentEntity;
import legal.shrinkwrap.api.persistence.entity.NormEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NormDocumentRepository extends JpaRepository<NormDocumentEntity, Long> {

    Optional<NormDocumentEntity> findByDocNumber(String docNumber);

    /**
     * Only the document numbers. Loading the entities would drag their html and their full text
     * along - @Basic(LAZY) needs bytecode enhancement, which this build does not do - and the
     * ASVG alone holds over 5000 of them.
     */
    @Query("SELECT d.docNumber FROM NormDocumentEntity d WHERE d.norm = :norm AND d.deletedAt IS NULL")
    List<String> findLiveDocNumbers(NormEntity norm);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE NormDocumentEntity d SET d.deletedAt = :deletedAt WHERE d.docNumber IN :docNumbers")
    int markDeleted(Collection<String> docNumbers, LocalDateTime deletedAt);

    /**
     * Every version of every provision that was in force at any point between the two days -
     * their period of validity has to overlap the one asked for. Passing the same day twice
     * yields the single version in force that day, so one query serves both questions.
     * <p>
     * Ausserkrafttreten is frequently not set, which is why it has to allow null: a provision
     * without an end date is in force, not expired.
     */
    @Query("""
            SELECT d FROM NormDocumentEntity d
            WHERE d.norm = :norm
              AND d.deletedAt IS NULL
              AND (d.inkrafttreten IS NULL OR d.inkrafttreten <= :until)
              AND (d.ausserkrafttreten IS NULL OR d.ausserkrafttreten > :from)
            ORDER BY d.sortIndex
            """)
    List<NormDocumentEntity> findInForceBetween(NormEntity norm, LocalDate from, LocalDate until);
}
