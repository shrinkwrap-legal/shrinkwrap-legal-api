package legal.shrinkwrap.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import legal.shrinkwrap.api.adapter.HtmlDownloadService;
import legal.shrinkwrap.api.adapter.ris.RisSearchParameterCaseLaw;
import legal.shrinkwrap.api.adapter.ris.RisSoapAdapter;
import legal.shrinkwrap.api.adapter.ris.dto.RisCourt;
import legal.shrinkwrap.api.adapter.ris.dto.RisJudikaturMetadaten;
import legal.shrinkwrap.api.adapter.ris.dto.RisJudikaturResult;
import legal.shrinkwrap.api.adapter.ris.dto.RisSearchResult;
import legal.shrinkwrap.api.dataset.CaseLawDataset;
import legal.shrinkwrap.api.dto.CaseLawRequestDto;
import legal.shrinkwrap.api.dto.CaseLawResponseDto;
import legal.shrinkwrap.api.persistence.entity.CaseLawEntity;
import legal.shrinkwrap.api.persistence.repo.CaseLawRepository;
import legal.shrinkwrap.api.python.ShrinkwrapPythonRestService;
import org.springframework.stereotype.Service;

import java.io.IOException;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    public DocumentServiceImpl(RisSoapAdapter risSoapAdapter, HtmlDownloadService htmlDownloadService, CaselawTextService caselawTextService, FileHandlingService fileHandlingService, ShrinkwrapPythonRestService shrinkwrapPythonRestService, CaseLawRepository caseLawRepository) {
        this.risSoapAdapter = risSoapAdapter;
        this.htmlDownloadService = htmlDownloadService;
        this.caselawTextService = caselawTextService;
        this.fileHandlingService = fileHandlingService;
        this.shrinkwrapPythonRestService = shrinkwrapPythonRestService;
        this.caseLawRepository = caseLawRepository;
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
            RisSearchParameterCaseLaw.RisSearchParameterCaseLawBuilder builder = RisSearchParameterCaseLaw.builder();
            builder.court(requestDto.court());
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
            CaseLawEntity entity = new CaseLawEntity();
            RisJudikaturMetadaten metadata = judikaturResult.getJudikaturMetadaten();
            //general metadata
            entity.setDocNumber(judikaturResult.getMetadaten().getId());
            entity.setApplicationType(judikaturResult.getMetadaten().getApplicationType().value());
            entity.setUrl(judikaturResult.getMetadaten().getUrl());
            entity.setHtmlUrl(judikaturResult.getHtmlDocumentUrl());
            entity.setLastChangedDate(judikaturResult.getMetadaten().getChanged());
            entity.setPublishedDate(judikaturResult.getMetadaten().getPublished());
            entity.setCourt(judikaturResult.getMetadaten().getOrgan());

            //judikatur specific metadata
            entity.setEcli(metadata.getEcli());
            entity.setCaseNumber(metadata.getGeschaeftszahl().getFirst());
            entity.setDecisionDate(metadata.getEntscheidungsdatum());

            entity.setMetadata(judikaturResult.getMetadaten().getFullResponseAsJson());

            caseLawEntity = caseLawRepository.save(entity);
        } else {
            caseLawEntity = dbEntity.get();
        }

        //try loading analysis from repository

        //if no match, make analysis, save to db


        String htmlContent = htmlDownloadService.downloadHtml(caseLawEntity.getHtmlUrl());

        CaseLawResponseDto res = caselawTextService.prepareRISCaseLawHtml(htmlContent);
        return res;
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
}