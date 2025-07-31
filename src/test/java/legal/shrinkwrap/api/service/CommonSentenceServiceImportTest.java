package legal.shrinkwrap.api.service;

import legal.shrinkwrap.api.adapter.ris.RisSearchParameterCaseLaw;
import legal.shrinkwrap.api.adapter.ris.RisSoapAdapter;
import legal.shrinkwrap.api.adapter.ris.dto.RisCourt;
import legal.shrinkwrap.api.adapter.ris.dto.RisSearchResult;
import legal.shrinkwrap.api.persistence.entity.CaseLawAnalysisEntity;
import legal.shrinkwrap.api.persistence.entity.CaseLawEntity;
import legal.shrinkwrap.api.persistence.repo.CaseLawAnalysisRepository;
import legal.shrinkwrap.api.utils.SentenceHashingTools;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest
@Tag("integration")
public class CommonSentenceServiceImportTest {

    @Autowired
    private CommonSentenceService commonSentenceService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private CaseLawAnalysisRepository caseLawAnalysisRepository;

    @Autowired
    private RisSoapAdapter risSoapAdapter;

    @Test
    public void testFindContainedSentences() {
        //import documents first
        RisSearchResult result1 = risSoapAdapter.findCaseLawDocuments(
                RisSearchParameterCaseLaw.builder()
                        .ecli("ECLI:AT:BVWG:2024:W287.2293603.1.00")
                        .court(RisCourt.BVwG)
                        .judikaturTyp(new RisSearchParameterCaseLaw.JudikaturTyp(false, true))
                        .build()
        );


        CaseLawEntity caseLawEntity = documentService.importJudikaturResult(result1.getJudikaturResults().getFirst());
        CaseLawAnalysisEntity analysisEntity = caseLawAnalysisRepository.findFirstByAnalysisTypeAndCaseLaw_IdOrderByAnalysisVersionDesc("text", caseLawEntity.getId()).get();


        String importString = "ECLI:AT:BVWG:2024:W287.2293603.1.00;Ba'V3?!]6%56p=,t\n" +
                "ECLI:AT:BVWG:2024:W287.2293603.1.00;qeY }~;0P$V@MX#a[8n_Q'7B b@eW)<&FO*gU]JqaD'\"#8z(,6Z#`W+V@~3DC~#?8\n" +
                "ECLI:AT:BVWG:2024:W287.2293603.1.00;/9[$o\"aP+2NTg>Q4^%L?KW*lMJ>d-0(uv&Cf1.5Q";

        commonSentenceService.importFromECLITextFile(importString);

        List<String> result = commonSentenceService.findContainedSentences(analysisEntity.getSentenceHash());
        assertEquals(3,result.size());
        String commonSentence = SentenceHashingTools.getCommonSentence(analysisEntity.getFullText(), result.get(0));
        assertTrue(commonSentence.trim().startsWith("Selbst für diejenigen, die nicht im Verdacht stehen"));
    }

}