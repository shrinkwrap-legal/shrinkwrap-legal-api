package legal.shrinkwrap.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import legal.shrinkwrap.api.adapter.ris.dto.RisCourt;
import legal.shrinkwrap.api.dto.CaseLawRequestDto;
import legal.shrinkwrap.api.dto.CaselawSummaryCivilCase;
import legal.shrinkwrap.api.persistence.entity.CaseLawAnalysisEntity;
import legal.shrinkwrap.api.persistence.entity.CaseLawEntity;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Tag("integration")
class CaselawVectorServiceTest {
    @Autowired
    private CaselawAnalyzerService caselawAnalyzerService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private CaselawVectorService caselawVectorService;

    @Test
    void getVectorForCase() throws IOException {
        URL resource = getClass().getClassLoader().getResource("demo-summary.json");
        String all = Resources.toString(resource, StandardCharsets.UTF_8);
        ObjectMapper objectMapper = new ObjectMapper();
        CaselawSummaryCivilCase caselawSummaryCivilCase = objectMapper.readValue(all, CaselawSummaryCivilCase.class);

        caselawVectorService.getVectorForCase(caselawSummaryCivilCase);
    }
}