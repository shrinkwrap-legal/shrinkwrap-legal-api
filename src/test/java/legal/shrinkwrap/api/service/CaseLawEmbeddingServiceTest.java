package legal.shrinkwrap.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import legal.shrinkwrap.api.adapter.ris.dto.RisCourt;
import legal.shrinkwrap.api.config.TestcontainersConfiguration;
import legal.shrinkwrap.api.dto.CaseLawRequestDto;
import legal.shrinkwrap.api.dto.CaselawSummaryCivilCase;
import legal.shrinkwrap.api.persistence.entity.CaseLawEntity;
import legal.shrinkwrap.api.persistence.repo.CaseLawRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@SpringBootTest
@Tag("integration")
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class CaseLawEmbeddingServiceTest {
    @Autowired
    private CaselawAnalyzerService caselawAnalyzerService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private CaseLawEmbeddingService caselawEmbeddingService;

    @Autowired
    private CaseLawRepository caseLawRepository;

    @Test
    void getVectorForCase() throws IOException {
        URL resource = getClass().getClassLoader().getResource("demo-summary.json");
        String all = Resources.toString(resource, StandardCharsets.UTF_8);
        ObjectMapper objectMapper = new ObjectMapper();
        CaselawSummaryCivilCase caselawSummaryCivilCase = objectMapper.readValue(all, CaselawSummaryCivilCase.class);

        caselawEmbeddingService.getVectorForCaseLawSummary(caselawSummaryCivilCase);


    }

    @Test
    void testSaveVectorForCase() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        CaseLawEntity cle1 = documentService.downloadCaseLaw(new CaseLawRequestDto(null, "JJT_20020625_OGH0002_0050OB00122_02F0000_000", RisCourt.Justiz, false));
        cle1 = caseLawRepository.save(cle1);
        URL resource = getClass().getClassLoader().getResource("demo-summary-JJT_20020625_OGH0002_0050OB00122_02F0000_000.json");
        String all = Resources.toString(resource, StandardCharsets.UTF_8);
        CaselawSummaryCivilCase cls1 = objectMapper.readValue(all, CaselawSummaryCivilCase.class);
        caselawEmbeddingService.getAndStoreVectorForCaseLawSummary(cle1, cls1);


        CaseLawEntity cle2 = documentService.downloadCaseLaw(new CaseLawRequestDto(null, "JJT_20090901_OGH0002_0050OB00154_09X0000_000", RisCourt.Justiz, false));
        resource = getClass().getClassLoader().getResource("demo-summary-JJT_20090901_OGH0002_0050OB00154_09X0000_000.json");
        all = Resources.toString(resource, StandardCharsets.UTF_8);
        CaselawSummaryCivilCase cls2 = objectMapper.readValue(all, CaselawSummaryCivilCase.class);
        cle2 = caseLawRepository.save(cle2);
        caselawEmbeddingService.getAndStoreVectorForCaseLawSummary(cle2, cls2);
        System.out.println(2);


    }
}