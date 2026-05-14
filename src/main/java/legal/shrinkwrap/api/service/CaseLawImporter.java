package legal.shrinkwrap.api.service;

import jakarta.annotation.PostConstruct;
import legal.shrinkwrap.api.adapter.ris.RisSearchParameterCaseLaw;
import legal.shrinkwrap.api.adapter.ris.RisSoapAdapter;
import legal.shrinkwrap.api.adapter.ris.dto.RisCourt;
import legal.shrinkwrap.api.adapter.ris.dto.RisJudikaturResult;
import legal.shrinkwrap.api.adapter.ris.dto.RisSearchResult;
import legal.shrinkwrap.api.persistence.entity.CaseLawEntity;
import legal.shrinkwrap.api.persistence.repo.CaseLawRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@Service
@AllArgsConstructor
@Slf4j
public class CaseLawImporter {
    private final DocumentService documentService;
    private final RisSoapAdapter risSoapAdapter;
    private final CaseLawRepository caseLawRepository;
    private final CaselawAnalyzerService caselawAnalyzerService;

    private final CommonSentenceService commonSentenceService;
    private final ResourceLoader resourceLoader;

    //@PostConstruct
    public void initDB() {
        log.info("performing initial import for all years up to current year");
        //change to actual last date
        for (int i=2026; i<=Year.now().getValue(); i++) {
            doInitialImportFor(Year.of(i));
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
        log.info("performing initial import for year " + year);
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

    @Scheduled(cron = "0 30 22 * * *")
    public void doSummariesForCases() {
        final String court = "OGH";
        final int maxParallel = 2;
        AtomicInteger completedTasks = new AtomicInteger(0);

        Page<CaseLawEntity> caseLawsWithoutSummary = caseLawRepository.findCaseLawWithoutSummary(PageRequest.of(0, 100), court);

        log.info("Starting summary generation for {} cases without summary", caseLawsWithoutSummary.getTotalElements());

        Function<CaseLawEntity, Boolean> fct= new Function<CaseLawEntity, Boolean>() {

            @Override
            public Boolean apply(CaseLawEntity entity) {
                //check if token limit still not reached
                if (CaselawAnalyzerService.AI_MODEL_FILL_DAILY_TOKENS < caselawAnalyzerService.getSpentTokensToday()) {
                    return false;
                }

                //check if still before 23:50 UTC
                if (LocalDateTime.now().isAfter(LocalDateTime.of(LocalDate.now(), LocalTime.of(23, 50)))) {
                    return false;
                }

                //if both conditions are met, then do summary
                documentService.getDocumentForEntity(entity, false);
                completedTasks.incrementAndGet();

                return true;
            }
        };

        //execute at most 10 at once
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var completionService = new ExecutorCompletionService<Boolean>(executor);
            Iterator<CaseLawEntity> iterator = caseLawsWithoutSummary.iterator();

            int runningTasks = 0;
            boolean shouldStop = false;

            while (runningTasks < maxParallel && iterator.hasNext()) {
                CaseLawEntity caseLaw = iterator.next();
                completionService.submit(() -> fct.apply(caseLaw));
                runningTasks++;
            }

            while (runningTasks > 0 && !shouldStop) {
                try {
                    Boolean result = completionService.take().get();
                    runningTasks--;

                    if (Boolean.FALSE.equals(result)) {
                        shouldStop = true;
                        executor.shutdown();
                        break;
                    }

                    if (iterator.hasNext()) {
                        CaseLawEntity caseLaw = iterator.next();
                        completionService.submit(() -> fct.apply(caseLaw));
                        runningTasks++;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    executor.shutdown();
                    break;
                } catch (ExecutionException e) {
                    log.error("Failed to create case law summary", e);
                    runningTasks--;
                }
            }

            log.info("Completed {} tasks out of {}", completedTasks.get(), caseLawsWithoutSummary.getTotalElements());
        }
    }
}
