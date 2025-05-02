package legal.shrinkwrap.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import legal.shrinkwrap.api.adapter.HtmlDownloadService;
import legal.shrinkwrap.api.adapter.ris.RisSearchParameterCaseLaw;
import legal.shrinkwrap.api.adapter.ris.RisSoapAdapter;
import legal.shrinkwrap.api.adapter.ris.dto.RisCourt;
import legal.shrinkwrap.api.adapter.ris.dto.RisJudikaturMetadaten;
import legal.shrinkwrap.api.adapter.ris.dto.RisJudikaturResult;
import legal.shrinkwrap.api.adapter.ris.dto.RisSearchResult;
import legal.shrinkwrap.api.adapter.ris.rest.dto.enums.OgdApplikationEnum;
import legal.shrinkwrap.api.dataset.CaseLawDataset;
import legal.shrinkwrap.api.dto.CaseLawRequestDto;
import legal.shrinkwrap.api.dto.CaseLawResponseDto;
import legal.shrinkwrap.api.dto.CaselawSummaryCivilCase;
import legal.shrinkwrap.api.persistence.entity.CaseLawAnalysisEntity;
import legal.shrinkwrap.api.persistence.entity.CaseLawEntity;
import legal.shrinkwrap.api.persistence.repo.CaseLawAnalysisRepository;
import legal.shrinkwrap.api.persistence.repo.CaseLawRepository;
import legal.shrinkwrap.api.python.ShrinkwrapPythonRestService;
import legal.shrinkwrap.api.utils.ObjectMapperWithXmlGregorianCalenderSupport;
import legal.shrinkwrap.api.utils.PandocTextWrapper;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DocumentServiceImpl implements DocumentService {

    private final RisSoapAdapter risSoapAdapter;

    private final HtmlDownloadService htmlDownloadService;

    private final CaselawTextService caselawTextService;

    private final FileHandlingService fileHandlingService;

    private final ShrinkwrapPythonRestService shrinkwrapPythonRestService;

    private final CaseLawRepository caseLawRepository;

    private final CaseLawAnalysisRepository caseLawAnalysisRepository;

    private final CaselawAnalyzerService caselawAnalyzerService;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${download-missing-judicature}")
    private Boolean downloadMissing;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public DocumentServiceImpl(RisSoapAdapter risSoapAdapter, HtmlDownloadService htmlDownloadService, CaselawTextService caselawTextService, FileHandlingService fileHandlingService, ShrinkwrapPythonRestService shrinkwrapPythonRestService, CaseLawRepository caseLawRepository, CaseLawAnalysisRepository caseLawAnalysisRepository, CaselawAnalyzerService caselawAnalyzerService) {
        this.risSoapAdapter = risSoapAdapter;
        this.htmlDownloadService = htmlDownloadService;
        this.caselawTextService = caselawTextService;
        this.fileHandlingService = fileHandlingService;
        this.shrinkwrapPythonRestService = shrinkwrapPythonRestService;
        this.caseLawRepository = caseLawRepository;
        this.caseLawAnalysisRepository = caseLawAnalysisRepository;
        this.caselawAnalyzerService = caselawAnalyzerService;
    }

    @Override
    public CaseLawResponseDto getDocument(CaseLawRequestDto requestDto) {
        CaseLawEntity caseLawEntity = null;
        Optional<CaseLawEntity> dbEntity = Optional.empty();

        //try loading from repository
        if (requestDto.ecli() != null) {
            dbEntity = caseLawRepository.findCaseLawEntityByEcli(requestDto.ecli());
        }
        if (dbEntity.isEmpty() && requestDto.docNumber() != null) {
            dbEntity = caseLawRepository.findCaseLawEntityByDocNumber(requestDto.docNumber());
        }

        //if no match, get from RIS, and save to db
        if (dbEntity.isEmpty()) {
            if (!downloadMissing) {
                //no downloading - so nothing to return here
                return null;
            }
            RisSearchParameterCaseLaw.RisSearchParameterCaseLawBuilder builder = RisSearchParameterCaseLaw.builder();
            builder.court(requestDto.court());
            builder.judikaturTyp(new RisSearchParameterCaseLaw.JudikaturTyp(false, true));
            if (requestDto.ecli() != null) {
                builder.ecli(requestDto.ecli());
            }
            else if (requestDto.docNumber() != null) {
                builder.docNumber(requestDto.docNumber());
            }

            RisSearchResult result = risSoapAdapter.findCaseLawDocuments(builder.build());
            if(result == null) {
                return null;
            }
            RisJudikaturResult judikaturResult = result.getJudikaturResults().getFirst();
            CaseLawEntity entity = mapJudikaturResultToEntity(judikaturResult);

            //html
            String htmlContent = htmlDownloadService.downloadHtml(entity.getHtmlUrl());
            CaseLawResponseDto dto = caselawTextService.prepareRISCaseLawHtml(htmlContent);
            entity.setFullCleanHtml(dto.caselawHtml());

            caseLawEntity = caseLawRepository.save(entity);
        } else {
            caseLawEntity = dbEntity.get();
        }

        Optional<CaseLawAnalysisEntity> textEntity = caseLawAnalysisRepository.findFirstByAnalysisTypeAndCaseLaw_IdOrderByAnalysisVersionDesc("text", caseLawEntity.getId());
        if (!textEntity.isPresent()) {
            CaseLawAnalysisEntity textAnalysisEntity = createTextConversion(caseLawEntity);
            caseLawAnalysisRepository.save(textAnalysisEntity);
            textEntity = Optional.of(textAnalysisEntity);
        }

        Optional<CaseLawAnalysisEntity> summary = caseLawAnalysisRepository.findFirstByAnalysisTypeAndCaseLaw_IdOrderByAnalysisVersionDesc("summary", caseLawEntity.getId());
        if (!summary.isPresent() && caseLawEntity.getApplicationType().equalsIgnoreCase(OgdApplikationEnum.Justiz.toString())) {
            //do it, save

            String text = textEntity.get().getFullText();
            CaselawSummaryCivilCase o = caselawAnalyzerService.summarizeCaselaw(text);
            if (o != null) {
                CaseLawAnalysisEntity analysisEntity = new CaseLawAnalysisEntity();
                analysisEntity.setCaseLaw(caseLawEntity);
                analysisEntity.setAnalysisType("summary");
                analysisEntity.setAnalysisVersion(1);
                try {
                    analysisEntity.setAnalysis(MAPPER.writeValueAsString(o));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                summary = Optional.of(caseLawAnalysisRepository.save(analysisEntity));
            }
        }

        Object summaryObj = null;
        if (summary.isPresent()) {
            try {
                summaryObj = MAPPER.readValue(summary.get().getAnalysis(), Object.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }


        CaseLawResponseDto res = new CaseLawResponseDto(textEntity.get().getWordCount(),null,summaryObj);
        return res;
    }

    public static CaseLawEntity mapJudikaturResultToEntity(RisJudikaturResult judikaturResult) {
        CaseLawEntity entity = new CaseLawEntity();
        RisJudikaturMetadaten metadata = judikaturResult.getJudikaturMetadaten();
        //general metadata
        entity.setDocNumber(judikaturResult.getMetadaten().getId());
        entity.setApplicationType(judikaturResult.getMetadaten().getApplicationType().value());
        entity.setUrl(judikaturResult.getMetadaten().getUrl());
        entity.setHtmlUrl(judikaturResult.getHtmlDocumentUrl());
        entity.setLastChangedDate(judikaturResult.getMetadaten().getChanged());
        entity.setPublishedDate(judikaturResult.getMetadaten().getPublished());
        entity.setOrgan(judikaturResult.getMetadaten().getOrgan());

        //judikatur specific metadata
        entity.setEcli(metadata.getEcli());
        entity.setCaseNumber(metadata.getGeschaeftszahl().getFirst());
        entity.setDecisionDate(metadata.getEntscheidungsdatum());

        if (judikaturResult.getJudikaturMetadaten().getVfghMetadaten() != null) {
            entity.setDecisionType(judikaturResult.getJudikaturMetadaten().getVfghMetadaten().getEntscheidungsart());
            entity.setCourt(judikaturResult.getJudikaturMetadaten().getVfghMetadaten().getGericht());
        }
        else if (judikaturResult.getJudikaturMetadaten().getVwghMetadaten() != null) {
            entity.setDecisionType(judikaturResult.getJudikaturMetadaten().getVwghMetadaten().getEntscheidungsart());
            entity.setCourt(judikaturResult.getJudikaturMetadaten().getVwghMetadaten().getGericht());
        }
        else if (judikaturResult.getJudikaturMetadaten().getLvwgMetadaten() != null) {
            entity.setDecisionType(judikaturResult.getJudikaturMetadaten().getLvwgMetadaten().getEntscheidungsart());
            entity.setCourt(judikaturResult.getJudikaturMetadaten().getLvwgMetadaten().getGericht());
        }
        else if (judikaturResult.getJudikaturMetadaten().getDskMetadaten() != null) {
            entity.setDecisionType(judikaturResult.getJudikaturMetadaten().getDskMetadaten().getEntscheidungsart());
            entity.setCourt(judikaturResult.getJudikaturMetadaten().getDskMetadaten().getGericht());
        }
        else if (judikaturResult.getJudikaturMetadaten().getBvwgMetadaten() != null) {
            entity.setDecisionType(judikaturResult.getJudikaturMetadaten().getBvwgMetadaten().getEntscheidungsart());
            entity.setCourt(judikaturResult.getJudikaturMetadaten().getBvwgMetadaten().getGericht());
        }
        else if (judikaturResult.getJudikaturMetadaten().getGbkMetadaten() != null) {
            entity.setDecisionType(judikaturResult.getJudikaturMetadaten().getGbkMetadaten().getEntscheidungsart());
            entity.setCourt(judikaturResult.getJudikaturMetadaten().getGbkMetadaten().getGericht());
        }
        else if (judikaturResult.getJudikaturMetadaten().getJustizMetadaten() != null) {
            entity.setDecisionType(judikaturResult.getJudikaturMetadaten().getJustizMetadaten().getEntscheidungsart());
            entity.setCourt(judikaturResult.getJudikaturMetadaten().getJustizMetadaten().getGericht());
        }


        entity.setMetadata(judikaturResult.getMetadaten().getFullResponseAsJson());

        return entity;
    }

    @Override
    public CaseLawDataset getCaselawDatasetForECLI(String ecli) {
        //ECLI:AT:VFGH:2024:E2499.2024
        //

        //GET some data from RIS
        RisSearchResult results = risSoapAdapter.findCaseLawDocuments(
                RisSearchParameterCaseLaw.builder()
                        .court(RisCourt.Justiz)
                        .ecli(ecli)
                        .judikaturTyp(new RisSearchParameterCaseLaw.JudikaturTyp(false, true))
                        .build()
        );



        if (results.getJudikaturResults().size() > 1) {
            throw new RuntimeException("More than one caselaw found for " + ecli);
        }

        RisJudikaturResult result = results.getJudikaturResults().getFirst();

        //maybe download HTML
        String fullHtml = fileHandlingService.loadFile(ecli, ".full.html");
        if (fullHtml == null) {
            fullHtml = htmlDownloadService.downloadHtml(result.getHtmlDocumentUrl());
            fileHandlingService.saveFile(result.getJudikaturMetadaten().getEcli(),".full.html",fullHtml);
        }

        //get "clean" html
        CaseLawResponseDto textDto = caselawTextService.prepareRISCaseLawHtml(fullHtml);

        //get text and sentences, if possible
        String textOnly = null;
        List<String> sentences = null;
        String sentencesFile = null;
        try {
            textOnly = fileHandlingService.loadFile(ecli, ".text.txt");
            if (textOnly == null) {
                textOnly = shrinkwrapPythonRestService.getTextFromHtml(textDto.caselawHtml());
                fileHandlingService.saveFile(ecli,".text.txt", textOnly);
            }

            sentencesFile = fileHandlingService.loadFile(ecli, ".sentences.txt");
            sentences = new ArrayList<>();
            if (sentencesFile == null) {
                sentences = shrinkwrapPythonRestService.getSentencesFromCaseLaw(textOnly);
                sentencesFile = String.join("\n", sentences);
                fileHandlingService.saveFile(ecli,".sentences.txt", sentencesFile);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //maybe sentences and text-only
        CaseLawDataset dataset = new CaseLawDataset(
                result.getMetadaten().getId(),
                result.getMetadaten().getApplicationType().value(),
                result.getMetadaten().getOrgan(),
                result.getMetadaten().getPublished(),
                result.getMetadaten().getChanged(),
                result.getMetadaten().getUrl(),
                result.getHtmlDocumentUrl(),
                String.join(";", result.getJudikaturMetadaten().getGeschaeftszahl()),
                result.getJudikaturMetadaten().getEcli(),
                null,
                null,
                null,
                textDto.caselawHtml(),
                sentencesFile);

        return dataset;
    }


    @Override
    public void importJudikaturResult(RisJudikaturResult result) {
        String docNumber = result.getMetadaten().getId();
        Optional<CaseLawEntity> dbEntity = caseLawRepository.findCaseLawEntityByDocNumber(docNumber);
        CaseLawEntity entity;
        if (dbEntity.isPresent()) {
            entity = dbEntity.get();
        } else {
            entity = mapJudikaturResultToEntity(result);
            entity = caseLawRepository.save(entity);
        }

        //check if html exists, otherwise download
        if (Strings.isEmpty(entity.getFullCleanHtml())) {
            String htmlContent = htmlDownloadService.downloadHtml(entity.getHtmlUrl());
            CaseLawResponseDto dto = caselawTextService.prepareRISCaseLawHtml(htmlContent);
            entity.setFullCleanHtml(dto.caselawHtml());

            CaseLawAnalysisEntity analysisEntity = createTextConversion(entity);

            entity = caseLawRepository.save(entity);
            analysisEntity = caseLawAnalysisRepository.save(analysisEntity);
        }
    }

    private CaseLawAnalysisEntity createTextConversion(CaseLawEntity entity) {
        String fullTextOnly = PandocTextWrapper.convertHtmlToText(entity.getFullCleanHtml());
        long wordCount = fullTextOnly.split(" ").length;
        CaseLawAnalysisEntity analysisEntity = new CaseLawAnalysisEntity();
        analysisEntity.setWordCount(wordCount);
        analysisEntity.setCaseLaw(entity);
        analysisEntity.setFullText(fullTextOnly);
        return analysisEntity;
    }


}
