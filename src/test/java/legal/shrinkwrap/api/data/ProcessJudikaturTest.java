package legal.shrinkwrap.api.data;


import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.ModelType;
import legal.shrinkwrap.api.SpringTest;
import legal.shrinkwrap.api.persistence.entity.CaseLawEntity;
import legal.shrinkwrap.api.service.CaselawAnalyzerService;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import legal.shrinkwrap.api.persistence.entity.CaseLawAnalysisEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
public class ProcessJudikaturTest extends SpringTest {

    @Autowired
    private CaselawAnalyzerService caselawAnalyzerService;

    private String resultFolder = "/Users/thomas/Downloads/ECLI/";

    private String selectedResults = "/Users/thomas/Downloads/ECLI/AT/";

    @Test
    public void testProcessJudikatur() throws IOException, InterruptedException {
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        Encoding enc = registry.getEncodingForModel(ModelType.GPT_4O_MINI);
        ObjectMapper objectMapper = YamlMapper.getMapper();

        log.info("iterate over all files");

        Iterator<File> iterator = FileUtils.iterateFiles(new File(selectedResults),null,true);

        Long totalToken = 0L;
        int count = 0;
        while (iterator.hasNext()) {
            File file = iterator.next();
            //


            try {
                CaseLawAnalysisEntity entity = objectMapper.readValue(file, CaseLawAnalysisEntity.class);
                int i = enc.countTokens(entity.getFullText());
                if (i<90000) {
                    caselawAnalyzerService.summarizeCaselaw(entity);
                    totalToken += i;
                } else {
                    System.out.println("skipped " + file.getAbsolutePath());
                }



                count++;
                if (count % 1 == 0) {
                    log.info(count + " File {}",file.getAbsolutePath());
                    log.info(totalToken + " token CaseLawAnalysisEntity {}",entity.getCaseLaw().getCaseNumber());
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                continue;
            }

        }

        System.out.println(count + " files, " + totalToken + " tokens");
    }

    @Test
    public void testProcessJudikaturToCsv() throws IOException, InterruptedException {
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        Encoding enc = registry.getEncodingForModel(ModelType.GPT_4O_MINI);
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectMapper yamlMapper = YamlMapper.getMapper();

        CsvMapper csvMapper = new CsvMapper();
        csvMapper.findAndRegisterModules();
        csvMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false); //Optional


        log.info("iterate over all files");
        Iterator<File> iterator = FileUtils.iterateFiles(new File(selectedResults),null,true);

        Long totalToken = 0L;
        int count = 0;

        List<CombinedCaseLawPojo> summaries = new ArrayList<>();

        while (iterator.hasNext()) {
            File file = iterator.next();
            try {
                CaseLawAnalysisEntity entity = yamlMapper.readValue(file, CaseLawAnalysisEntity.class);
                CaseLawEntity caseLaw = entity.getCaseLaw();
                CombinedCaseLawPojo pojo = new CombinedCaseLawPojo();
                pojo.setCourt(caseLaw.getCourt());
                pojo.setEcli(caseLaw.getEcli());
                pojo.setCaseNumber(caseLaw.getCaseNumber());
                pojo.setWordCount(entity.getWordCount());
                pojo.setDecisionDate(caseLaw.getDecisionDate());
                pojo.setPublishedDate(caseLaw.getPublishedDate());

                int i = enc.countTokens(entity.getFullText());
                pojo.setToken(i);

                pojo.setApplicationType(caseLaw.getApplicationType());
                pojo.setDocNumber(caseLaw.getDocNumber());
                summaries.add(pojo);


            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }

        // Define schema for CSV
        CsvSchema schema = csvMapper.schemaFor(CombinedCaseLawPojo.class).withHeader();

        // Write list to CSV file
        try {
            csvMapper.writer(schema).writeValue(new File("case_law_summaries.csv"), summaries);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(count + " files, " + totalToken + " tokens");
    }

    @Getter
    @Setter
    public static class CombinedCaseLawPojo {

        // Fields from CaseLawEntity
        private Long id;

        private String ecli;
        private String docNumber;
        private String caseNumber;
        private String applicationType;
        private String court;
        private String url;
        private String htmlUrl;
        private LocalDate decisionDate;
        private LocalDate publishedDate;
        private LocalDate lastChangedDate;
        private String metadata;
        private String fullCleanHtml;

        // Fields from CaseLawAnalysisEntity
        private Long wordCount;
        private Integer token;
    }
}
