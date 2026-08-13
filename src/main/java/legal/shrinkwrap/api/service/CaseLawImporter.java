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
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
@AllArgsConstructor
@Slf4j
public class CaseLawImporter {
    //the daily import retries failing RIS calls until this time, the rest is left to the next run
    private static final ZoneId RIS_ZONE = ZoneId.of("Europe/Vienna");
    private static final LocalTime IMPORT_DEADLINE = LocalTime.of(16, 0);
    private static final Duration FIRST_BACKOFF = Duration.ofMinutes(1);
    private static final Duration MAX_BACKOFF = Duration.ofMinutes(30);

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
        ZonedDateTime deadline = ZonedDateTime.now(RIS_ZONE).with(IMPORT_DEADLINE);

        for (RisCourt court : RisCourt.values()) {
            RisSearchResult results = retryOnRisError("search for " + court.name(), deadline,
                    () -> risSoapAdapter.findCaseLawDocuments(
                            RisSearchParameterCaseLaw.builder()
                                    .court(court)
                                    .changedInLastXDays(5)
                                    .judikaturTyp(new RisSearchParameterCaseLaw.JudikaturTyp(false, true))
                                    .build()
                    ));
            if (results == null) {
                continue;
            }

            for (RisJudikaturResult result : results.getJudikaturResults()) {
                retryOnRisError("import of " + result.getMetadaten().getId(), deadline,
                        () -> documentService.importJudikaturResult(result));
            }
            log.info("import done for " + court.name() + " with " + results.getJudikaturResults().size() + " documents");
        }
    }

    /**
     * Retries a failing RIS call with a wait doubling from one minute up to {@link #MAX_BACKOFF}, so that the
     * server can recover and a soft-ban of our address expires. Gives up when the next wait would end after the
     * deadline, the missing documents are then picked up by the next daily run. 4xx answers (e.g. 404 for a
     * document that is gone) are not retried.
     *
     * @return the result of the call, or null if it kept failing
     */
    private <T> T retryOnRisError(String description, ZonedDateTime deadline, Supplier<T> call) {
        Duration wait = FIRST_BACKOFF;

        //try again and again until the call succeeds, the deadline is reached or retrying is pointless
        while (true) {
            try {
                return call.get();
            } catch (HttpClientErrorException e) {
                //4xx, e.g. 404 for a document that is gone - that will not get better by waiting
                log.error("Skipping {}, RIS answered {}", description, e.getStatusCode(), e);
                if (e.getStatusCode().value() != 429) {
                    // retry like transient RIS error
                    return null;
                }
            } catch (RuntimeException e) {
                if (ZonedDateTime.now(RIS_ZONE).plus(wait).isAfter(deadline)) {
                    log.error("Skipping {}, still failing at the deadline {}", description, deadline, e);
                    return null;
                }

                log.warn("Retrying {} in {} minutes: {}", description, wait.toMinutes(), e.toString());
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return null;
                }

                //wait twice as long before the next try, but never longer than MAX_BACKOFF
                wait = wait.multipliedBy(2);
                if (wait.compareTo(MAX_BACKOFF) > 0) {
                    wait = MAX_BACKOFF;
                }
            }
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
        doSummariesForCases(List.of("OGH"));
    }

    public void doSummariesForCases(Collection<String> courts) {
        if (courts == null || courts.isEmpty()) {
            log.warn("Skipping summary generation, no courts given");
            return;
        }

        final int maxParallel = 2;
        final int pageSize = 100;
        //end of the daily time window, taken once so that a run reaching into the next day also stops
        final LocalDateTime deadline = LocalDateTime.of(LocalDate.now(), LocalTime.of(23, 50));
        //cases already tried in this run - a failing case stays in the query result and would be retried endlessly
        final Set<Long> attemptedCaseIds = new HashSet<>();
        AtomicInteger createdSummaries = new AtomicInteger(0);
        AtomicInteger failedSummaries = new AtomicInteger(0);

        Function<CaseLawEntity, Boolean> fct = entity -> {
            //check if token limit still not reached
            if (CaselawAnalyzerService.AI_MODEL_FILL_DAILY_TOKENS < caselawAnalyzerService.getSpentTokensToday()) {
                log.info("Stopping summary generation, daily token limit reached");
                return false;
            }

            //check if still before the deadline
            if (LocalDateTime.now().isAfter(deadline)) {
                log.info("Stopping summary generation, deadline {} reached", deadline);
                return false;
            }

            //if both conditions are met, then do summary
            try {
                documentService.getDocumentForEntity(entity, false);
                createdSummaries.incrementAndGet();
            } catch (Exception e) {
                //a single broken case must not stop the whole run
                log.error("Failed to create case law summary for id {} / ecli {}", entity.getId(), entity.getEcli(), e);
                failedSummaries.incrementAndGet();
            }

            return true;
        };

        boolean shouldStop = false;
        int pageNumber = 0;
        int processedPages = 0;
        Page<CaseLawEntity> page = findCaseLawWithoutSummary(courts, pageNumber, pageSize);

        log.info("Starting summary generation for courts {}, {} cases without summary, {} results per page",
                courts, page.getTotalElements(), pageSize);

        while (!shouldStop && page.hasContent()) {
            List<CaseLawEntity> caseLaws = page.getContent().stream()
                    .filter(caseLaw -> attemptedCaseIds.add(caseLaw.getId()))
                    .toList();

            if (caseLaws.isEmpty()) {
                //this page only holds cases that already failed in this run, so continue with the next one
                log.warn("All {} results of page {} already failed in this run, continuing with page {}",
                        page.getNumberOfElements(), pageNumber, pageNumber + 1);
                pageNumber++;
                page = findCaseLawWithoutSummary(courts, pageNumber, pageSize);
                continue;
            }

            //execute at most maxParallel at once
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                var completionService = new ExecutorCompletionService<Boolean>(executor);
                Iterator<CaseLawEntity> iterator = caseLaws.iterator();

                int runningTasks = 0;

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
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        executor.shutdown();
                        shouldStop = true;
                        break;
                    } catch (ExecutionException e) {
                        log.error("Failed to create case law summary", e);
                        failedSummaries.incrementAndGet();
                        runningTasks--;
                    }

                    if (!shouldStop && iterator.hasNext()) {
                        CaseLawEntity caseLaw = iterator.next();
                        completionService.submit(() -> fct.apply(caseLaw));
                        runningTasks++;
                    }
                }
            }

            processedPages++;
            log.info("Page {} finished, {} of its {} results processed, {} summaries created and {} failed in this run",
                    pageNumber, caseLaws.size(), page.getNumberOfElements(), createdSummaries.get(), failedSummaries.get());

            if (!shouldStop) {
                page = findCaseLawWithoutSummary(courts, pageNumber, pageSize);
            }
        }

        log.info("Summary generation finished, {} summaries created, {} failed, {} pages processed, {} cases without summary left",
                createdSummaries.get(), failedSummaries.get(), processedPages, caseLawRepository.countCaseLawWithoutSummary(courts));
    }

    private Page<CaseLawEntity> findCaseLawWithoutSummary(Collection<String> courts, int pageNumber, int pageSize) {
        return caseLawRepository.findCaseLawWithoutSummary(
                PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.ASC, "id")), courts);
    }
}
