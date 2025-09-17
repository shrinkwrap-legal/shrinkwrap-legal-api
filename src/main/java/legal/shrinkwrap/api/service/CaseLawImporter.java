package legal.shrinkwrap.api.service;

import jakarta.annotation.PostConstruct;
import legal.shrinkwrap.api.adapter.ris.RisSearchParameterCaseLaw;
import legal.shrinkwrap.api.adapter.ris.RisSoapAdapter;
import legal.shrinkwrap.api.adapter.ris.dto.RisCourt;
import legal.shrinkwrap.api.adapter.ris.dto.RisJudikaturResult;
import legal.shrinkwrap.api.adapter.ris.dto.RisSearchResult;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Year;

@Service
@AllArgsConstructor
@Slf4j
public class CaseLawImporter {
    private final DocumentService documentService;
    private final RisSoapAdapter risSoapAdapter;

    private final CommonSentenceService commonSentenceService;
    private final ResourceLoader resourceLoader;

    //@PostConstruct
    public void initDB() {
        //change to actual last date
        for (int i=1800; i<Year.now().getValue(); i++) {
            doInitialImportFor(Year.of(2021));
        }
    }

    //@PostConstruct
    public void redoText() {
        new Thread(() -> documentService.regenerateTextConversion(false)).start();
    }

    //@PostConstruct
    public void importCommonSentences() throws IOException {
        Resource resource = resourceLoader.getResource("classpath:common-sentences.txt");
        String s = IOUtils.toString(resource.getInputStream(), StandardCharsets.UTF_8);
        new Thread(() -> commonSentenceService.importFromECLITextFile(s)).start();
    }


    @Scheduled(cron = "0 30 3 * * *")
    public void updateLatestDocuments() {
        for (RisCourt court : RisCourt.values()) {
            RisSearchResult results = risSoapAdapter.findCaseLawDocuments(
                    RisSearchParameterCaseLaw.builder()
                            .court(court)
                            .changedInLastXDays(5)
                            .judikaturTyp(new RisSearchParameterCaseLaw.JudikaturTyp(false, true))
                            .build()
            );

            for (RisJudikaturResult result : results.getJudikaturResults()) {
                documentService.importJudikaturResult(result);
            }
            log.info("import done for " + court.name() + " with " + results.getJudikaturResults().size() + " documents");
        }
    }

    public void doInitialImportFor(Year year) {
        for (RisCourt court : RisCourt.values()) {
            RisSearchResult results = risSoapAdapter.findCaseLawDocuments(
                    RisSearchParameterCaseLaw.builder()
                            .court(court)
                            .year(year)
                            .judikaturTyp(new RisSearchParameterCaseLaw.JudikaturTyp(false, true))
                            .build()
            );

            for (RisJudikaturResult result : results.getJudikaturResults()) {
                documentService.importJudikaturResult(result);
            }
        }
    }
}
