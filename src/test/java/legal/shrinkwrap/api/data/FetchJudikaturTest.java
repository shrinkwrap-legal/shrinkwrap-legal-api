package legal.shrinkwrap.api.data;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import legal.shrinkwrap.api.adapter.HtmlDownloadService;
import legal.shrinkwrap.api.adapter.ris.RisSearchParameterCaseLaw;
import legal.shrinkwrap.api.adapter.ris.RisSearchParameterCaseLawBuilder;
import legal.shrinkwrap.api.adapter.ris.RisSoapAdapterImpl;
import legal.shrinkwrap.api.adapter.ris.dto.RisCourt;
import legal.shrinkwrap.api.adapter.ris.dto.RisSearchResult;
import legal.shrinkwrap.api.config.AdapterConfiguration;
import legal.shrinkwrap.api.config.ServiceConfiguration;
import legal.shrinkwrap.api.dataset.CaseLawDataset;
import legal.shrinkwrap.api.service.CaselawTextService;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {AdapterConfiguration.class, ServiceConfiguration.class})
public class FetchJudikaturTest {

    @Autowired
    private RisSoapAdapterImpl risSoapAdapter;

    @Autowired
    private HtmlDownloadService htmlDownloadService;

    private CaselawTextService caselawTextService = new CaselawTextService();

    @Test
    public void test_getJustiz() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());


        RisSearchResult result = risSoapAdapter.findCaseLawDocuments(
                RisSearchParameterCaseLawBuilder.builder()
                        .court(RisCourt.Justiz)
                        .judikaturTyp(new RisSearchParameterCaseLaw.JudikaturTyp(false, true))
                        .build()
        );
        assertThat(result).isNotNull();
        assertThat(result.getJudikaturResults()).isNotNull().hasSize(1000);

        List<CaseLawDataset> dataset = new ArrayList<>();
        result.getJudikaturResults().forEach(r -> {
            String fullHtml = htmlDownloadService.downloadHtml(r.getHtmlDocumentUrl());
            String content = caselawTextService.extractContent(fullHtml);
            dataset.add(new CaseLawDataset(
                    r.getMetadaten().getId(),
                    r.getMetadaten().getApplicationType().value(),
                    r.getMetadaten().getOrgan(),
                    r.getMetadaten().getPublished(),
                    r.getMetadaten().getChanged(), r.getMetadaten().getUrl(), r.getHtmlDocumentUrl(), String.join(";", r.getJudikaturMetadaten().getGeschaeftszahl()),
                    null, null, null, null, content));

        });

        String jsonResult = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dataset);

        /*
        try {
            FileUtils.writeStringToFile(ResourceUtils.getFile("dataset/justiz.json"), jsonResult, Charset.defaultCharset());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
         */


        System.out.println(jsonResult);
    }
}
