package legal.shrinkwrap.api.data;


import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import legal.shrinkwrap.api.SpringTest;
import legal.shrinkwrap.api.adapter.ris.dto.RisJudikaturResult;
import legal.shrinkwrap.api.persistence.entity.CaseLawAnalysisEntity;
import legal.shrinkwrap.api.persistence.entity.CaseLawEntity;
import legal.shrinkwrap.api.python.ShrinkwrapPythonRestService;
import legal.shrinkwrap.api.service.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.ResourceUtils;

import legal.shrinkwrap.api.adapter.HtmlDownloadService;
import legal.shrinkwrap.api.adapter.ris.RisSearchParameterCaseLaw;
import legal.shrinkwrap.api.adapter.ris.RisSoapAdapterImpl;
import legal.shrinkwrap.api.adapter.ris.dto.RisCourt;
import legal.shrinkwrap.api.adapter.ris.dto.RisSearchResult;
import legal.shrinkwrap.api.dataset.CaseLawDataset;
import legal.shrinkwrap.api.dto.CaseLawResponseDto;

@SpringBootTest
@Slf4j
public class FetchJudikaturTest extends SpringTest {

    @Autowired
    private RisSoapAdapterImpl risSoapAdapter;

    @Autowired
    private HtmlDownloadService htmlDownloadService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private CaselawAnalyzerService caselawAnalyzerService;

    @Autowired
    private ShrinkwrapPythonRestService shrinkwrapPythonRestService;

    //@Value("${files.output-directory}")
    private String outputDirectory = "/tmp/shrinkwrap/";

    private CaselawTextService caselawTextService = new CaselawTextService();

    @Autowired
    private FileHandlingService fileHandlingService;

    @Test
    public void test_getJustizAndSingleHtml() {
        RisSearchResult result = risSoapAdapter.findCaseLawDocuments(
                RisSearchParameterCaseLaw.builder()
                        .court(RisCourt.Justiz)
                        .judikaturTyp(new RisSearchParameterCaseLaw.JudikaturTyp(false, true))
                        .build()
        );
        assertThat(result).isNotNull();
        assertThat(result.getJudikaturResults()).isNotNull().hasSize(1000);

        String fullHtml = htmlDownloadService.downloadHtml(result.getJudikaturResults().getFirst().getHtmlDocumentUrl());
        CaseLawResponseDto content = caselawTextService.prepareRISCaseLawHtml(fullHtml);
        assertThat(content).isNotNull();
        assertThat(content.caselawHtml()).isNotNull();
    }

    @Test
    @Disabled
    public void test_getJustizDataSet() throws IOException {
        ObjectMapper objectMapper = YamlMapper.getMapper();

        RisSearchResult results = risSoapAdapter.findCaseLawDocuments(
                RisSearchParameterCaseLaw.builder()
                        .court(RisCourt.BVwG)
                        .judikaturTyp(new RisSearchParameterCaseLaw.JudikaturTyp(false, true))
                        .build()
        );
        assertThat(results).isNotNull();

        //download, put in DB
        int cnt = 0;
        for (RisJudikaturResult result : results.getJudikaturResults()) {
            cnt++;
            if (cnt % 20 == 0) {
                log.info(cnt + " out of " + results.getJudikaturResults().size() + " = " + (cnt *100 / results.getJudikaturResults().size() + " %"));
            }
            CaseLawEntity entity = DocumentServiceImpl.mapJudikaturResultToEntity(result);
            if (entity.getEcli() == null) {
                System.out.println(entity);
                continue;
            }

            if (fileHandlingService.loadFile(entity.getEcli(),"all.yaml") != null) {
                continue;
            }
            log.info("new " + entity.getEcli());

            String htmlContent = htmlDownloadService.downloadHtml(entity.getHtmlUrl());
            CaseLawResponseDto dto = caselawTextService.prepareRISCaseLawHtml(htmlContent);
            entity.setFullCleanHtml(dto.caselawHtml());

            //text only
            String textFromHtml = shrinkwrapPythonRestService.getTextFromHtml(dto.caselawHtml());
            CaseLawAnalysisEntity aEntity = new CaseLawAnalysisEntity();
            aEntity.setFullText(textFromHtml);
            aEntity.setCaseLaw(entity);
            aEntity.setWordCount((long) textFromHtml.split("\\s").length);

            String yaml = objectMapper.writeValueAsString(aEntity);

            //save as single yaml
            fileHandlingService.saveFile(entity.getEcli(),"all.yaml",yaml);
        }


    }

    @Test
    @Disabled
    public void test_getJustiz() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());


        RisSearchResult result = risSoapAdapter.findCaseLawDocuments(
                        RisSearchParameterCaseLaw.builder()
                        .court(RisCourt.Justiz)
                        .judikaturTyp(new RisSearchParameterCaseLaw.JudikaturTyp(false, true))
                        .build()
        );
        assertThat(result).isNotNull();
        assertThat(result.getJudikaturResults()).isNotNull().hasSize(1000);

        List<CaseLawDataset> dataset = new ArrayList<>();
        result.getJudikaturResults().forEach(r -> {
            String fullHtml = htmlDownloadService.downloadHtml(r.getHtmlDocumentUrl());
            CaseLawResponseDto content = caselawTextService.prepareRISCaseLawHtml(fullHtml);
            dataset.add(new CaseLawDataset(
                    r.getMetadaten().getId(),
                    r.getMetadaten().getApplicationType().value(),
                    r.getMetadaten().getOrgan(),
                    r.getMetadaten().getPublished(),
                    r.getMetadaten().getChanged(), r.getMetadaten().getUrl(), r.getHtmlDocumentUrl(), String.join(";", r.getJudikaturMetadaten().getGeschaeftszahl()),
                    r.getJudikaturMetadaten().getEcli(), null, null, null, content.caselawHtml(), null));

        });



        String jsonResult = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dataset);

        dataset.stream().forEach(d -> {
            try {
                FileUtils.writeStringToFile(ResourceUtils.getFile(outputDirectory + File.separator + d.caseLawEcli().replaceAll(":","_") + ".html"), d.contentHtml(), Charset.forName("UTF-8"));
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        });

        try {
            FileUtils.writeStringToFile(ResourceUtils.getFile("dataset/justiz.json"), jsonResult, Charset.defaultCharset());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }


    }
}
