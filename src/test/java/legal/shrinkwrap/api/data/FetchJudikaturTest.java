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
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.ResourceUtils;

import legal.shrinkwrap.api.adapter.HtmlDownloadService;
import legal.shrinkwrap.api.adapter.ris.RisSearchParameterCaseLaw;
import legal.shrinkwrap.api.adapter.ris.RisSoapAdapterImpl;
import legal.shrinkwrap.api.adapter.ris.dto.RisCourt;
import legal.shrinkwrap.api.adapter.ris.dto.RisSearchResult;
import legal.shrinkwrap.api.config.AdapterConfiguration;
import legal.shrinkwrap.api.config.CommonServiceConfiguration;
import legal.shrinkwrap.api.dataset.CaseLawDataset;
import legal.shrinkwrap.api.dto.CaseLawResponseDto;
import legal.shrinkwrap.api.service.CaselawTextService;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {AdapterConfiguration.class, CommonServiceConfiguration.class})
public class FetchJudikaturTest {

    @Autowired
    private RisSoapAdapterImpl risSoapAdapter;

    @Autowired
    private HtmlDownloadService htmlDownloadService;

    //@Value("${files.output-directory}")
    private String outputDirectory = "/tmp/shrinkwrap/";

    private CaselawTextService caselawTextService = new CaselawTextService();

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
