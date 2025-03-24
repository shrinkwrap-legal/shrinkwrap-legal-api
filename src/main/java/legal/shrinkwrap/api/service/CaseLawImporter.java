package legal.shrinkwrap.api.service;

import legal.shrinkwrap.api.adapter.ris.RisSearchParameterCaseLaw;
import legal.shrinkwrap.api.adapter.ris.RisSoapAdapter;
import legal.shrinkwrap.api.adapter.ris.dto.RisCourt;
import legal.shrinkwrap.api.adapter.ris.dto.RisJudikaturResult;
import legal.shrinkwrap.api.adapter.ris.dto.RisSearchResult;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Year;

@Service
@AllArgsConstructor
public class CaseLawImporter {
    private final DocumentService documentService;
    private final RisSoapAdapter risSoapAdapter;

    //@PostConstruct
    public void initDB() {
        //change to actual last date
        for (int i=1800; i<Year.now().getValue(); i++) {
            doInitialImportFor(Year.of(2021));
        }
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
