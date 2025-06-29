package legal.shrinkwrap.api.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import legal.shrinkwrap.api.adapter.ris.dto.RisCourt;
import legal.shrinkwrap.api.dto.CaseLawRequestDto;
import legal.shrinkwrap.api.dto.CaseLawResponseDto;
import legal.shrinkwrap.api.dto.CaselawSummaryCivilCase;
import legal.shrinkwrap.api.persistence.entity.CaseLawAnalysisEntity;
import legal.shrinkwrap.api.persistence.entity.CaseLawEntity;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import legal.shrinkwrap.api.adapter.HtmlDownloadService;
import legal.shrinkwrap.api.dataset.CaseLawDataset;


@SpringBootTest
@Tag("integration")
class CaselawAnalyzerServiceTest {

    @Autowired
    private CaselawAnalyzerService caselawAnalyzerService;

    @Autowired
    private FileHandlingService fileHandlingService;

    @Autowired
    private HtmlDownloadService htmlDownloadService;

    @Autowired
    private CaselawTextService caselawTextService;

    @Autowired
    private DocumentService documentService;

    @Test
    public void singleCaseLaw() {
        String ecli = "ECLI:AT:OGH0002:2024:008OBA00004";
        CaseLawDataset caselawDatasetForECLI = documentService.getCaselawDatasetForECLI(ecli);
        caselawAnalyzerService.summarizeCaselaw(caselawDatasetForECLI.sentences());
        caselawAnalyzerService.analyzeCaselaw(caselawDatasetForECLI);
    }

    @Test
    public void singleCaseLawSummary() throws InterruptedException {
        String ecli = "ECLI:AT:OGH0002:2024:008OBA00004";
        CaseLawRequestDto dto = new CaseLawRequestDto(ecli, null, RisCourt.Justiz);
        CaseLawEntity entity = documentService.downloadCaseLaw(dto);
        CaseLawAnalysisEntity analysisEntity = DocumentServiceImpl.createTextConversion(entity);
        CaselawSummaryCivilCase caselawSummaryCivilCase = caselawAnalyzerService.summarizeCaselaw(analysisEntity.getFullText()).summary();
        System.out.println(caselawSummaryCivilCase.toString());

    }

    @Test
    public void singleCaseLawSummaryEuGH() throws InterruptedException {
        String ecli = "ECLI:AT:OGH0002:2025:0080OB00021.25H.0526.000";
        CaseLawRequestDto dto = new CaseLawRequestDto(ecli, null, RisCourt.Justiz);
        CaseLawEntity entity = documentService.downloadCaseLaw(dto);
        CaseLawAnalysisEntity analysisEntity = DocumentServiceImpl.createTextConversion(entity);
        CaselawSummaryCivilCase caselawSummaryCivilCase = caselawAnalyzerService.summarizeCaselaw(analysisEntity.getFullText()).summary();
        System.out.println(caselawSummaryCivilCase.toString());

    }
    @Test
    public void singleCaseLawSummaryVfGH() throws InterruptedException {
        String ecli = "ECLI:AT:VFGH:2016:G7.2016";
        CaseLawRequestDto dto = new CaseLawRequestDto(ecli, null, RisCourt.VfGH);
        CaseLawEntity entity = documentService.downloadCaseLaw(dto);
        CaseLawAnalysisEntity analysisEntity = DocumentServiceImpl.createTextConversion(entity);
        CaselawSummaryCivilCase caselawSummaryCivilCase = caselawAnalyzerService.summarizeCaselaw(analysisEntity.getFullText()).summary();
        System.out.println(caselawSummaryCivilCase.toString());

    }

    @Test
    void analyzeCaselaw() {
        Path directoryPath = Paths.get("c:\\tmp\\shrinkwrap");

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directoryPath, "*.html")) {
            for (Path entry : stream) {
                System.out.println("Reading file: " + entry.getFileName());
                String sentenceContent = null;// Files.readString((Paths.get("c:\\tmp\\shrinkwrap\\ECLI_AT_OGH0002_2024_008OBA00004.24G.0826.000.html.sentences.txt")), StandardCharsets.UTF_8);
                String htmlContent = Files.readString((Paths.get("c:\\tmp\\shrinkwrap\\ECLI_AT_OGH0002_2024_008OBA00004.24G.0826.000.html")), StandardCharsets.UTF_8);
                CaseLawDataset ds = new CaseLawDataset(null,null,null,null,null,null,null,null,null,null,null,null,htmlContent,sentenceContent);
                caselawAnalyzerService.summarizeCaselaw(ds.sentences());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}