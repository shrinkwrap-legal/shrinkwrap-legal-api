package legal.shrinkwrap.api.service;

import jakarta.transaction.Transactional;
import legal.shrinkwrap.api.SpringTest;
import legal.shrinkwrap.api.adapter.ris.RisSearchParameterCaseLaw;
import legal.shrinkwrap.api.adapter.ris.RisSoapAdapter;
import legal.shrinkwrap.api.adapter.ris.dto.RisCourt;
import legal.shrinkwrap.api.adapter.ris.dto.RisSearchResult;
import legal.shrinkwrap.api.dto.CaseLawResponseDto;
import legal.shrinkwrap.api.dto.CaseLawRequestDto;
import legal.shrinkwrap.api.persistence.entity.CaseLawAnalysisEntity;
import legal.shrinkwrap.api.persistence.entity.CaseLawEntity;
import legal.shrinkwrap.api.persistence.repo.CaseLawAnalysisRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class DocumentServiceImplTest extends SpringTest {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private RisSoapAdapter risSoapAdapter;

    @Autowired
    private CaseLawAnalysisRepository caseLawAnalysisRepository;


    @Test
    void getDocument() {
        String docNumber="JJT_20250326_OGH0002_0060OB00047_25T0000_000";
        CaseLawRequestDto dto = new CaseLawRequestDto(null, docNumber, RisCourt.Justiz, false);
        CaseLawResponseDto document = documentService.getDocument(dto);
        document = documentService.getDocument(dto);
    }

    @Test
    void testGetDatasetForEcli() {
        String ecli = "ECLI:AT:OGH0002:2024:008OBA00004";
        CaseLawRequestDto dto = new CaseLawRequestDto(ecli, null, null, false);
        documentService.getCaselawDatasetForECLI(ecli);
    }

    @Test
    @Transactional
    void testImportIdenticalJudikaturResults() {
        RisSearchResult result1 = risSoapAdapter.findCaseLawDocuments(
                RisSearchParameterCaseLaw.builder()
                        .ecli("ECLI:AT:BVWG:2025:L508.2308074.1.00")
                        .court(RisCourt.BVwG)
                        .judikaturTyp(new RisSearchParameterCaseLaw.JudikaturTyp(false, true))
                        .build()
        );

        RisSearchResult result2 = risSoapAdapter.findCaseLawDocuments(
                RisSearchParameterCaseLaw.builder()
                        .ecli("ECLI:AT:BVWG:2025:L508.2308071.1.00")
                        .court(RisCourt.BVwG)
                        .judikaturTyp(new RisSearchParameterCaseLaw.JudikaturTyp(false, true))
                        .build()
        );


        CaseLawEntity entity1 = documentService.importJudikaturResult(result1.getJudikaturResults().getFirst());
        CaseLawEntity entity2 = documentService.importJudikaturResult(result2.getJudikaturResults().getFirst());

        //try lookup of analysis - should be set
        Optional<CaseLawAnalysisEntity> fullText1 = caseLawAnalysisRepository.findFirstByAnalysisTypeAndCaseLaw_IdOrderByAnalysisVersionDesc("text", entity1.getId());
        Optional<CaseLawAnalysisEntity> fullText2 = caseLawAnalysisRepository.findFirstByAnalysisTypeAndCaseLaw_IdOrderByAnalysisVersionDesc("text", entity2.getId());
        assertEquals(fullText2.get().getIdenticalTo(), entity1);
    }
}