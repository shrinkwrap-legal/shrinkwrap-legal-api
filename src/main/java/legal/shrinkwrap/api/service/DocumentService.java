package legal.shrinkwrap.api.service;

import legal.shrinkwrap.api.adapter.ris.dto.RisCourt;
import legal.shrinkwrap.api.adapter.ris.dto.RisJudikaturResult;
import legal.shrinkwrap.api.dataset.CaseLawDataset;
import legal.shrinkwrap.api.dto.CaseLawFullTextDto;
import legal.shrinkwrap.api.dto.CaseLawResponseDto;
import legal.shrinkwrap.api.dto.CaseLawRequestDto;
import legal.shrinkwrap.api.persistence.entity.CaseLawEntity;

import java.time.LocalDate;
import java.util.List;


public interface DocumentService {

    CaseLawResponseDto getDocument(CaseLawRequestDto caseLawRequestDto);

    CaseLawResponseDto getDocumentForEntity(CaseLawEntity caseLawEntity, boolean includePrompts);

    CaseLawEntity downloadCaseLaw(CaseLawRequestDto requestDto);

    List<CaseLawResponseDto> findCaseLaw(String search, RisCourt court, LocalDate dateFrom, LocalDate dateTo);

    CaseLawFullTextDto getFullTextForEcli(String ecli);

    CaseLawDataset getCaselawDatasetForECLI(String ecli);

    CaseLawEntity importJudikaturResult(RisJudikaturResult result);

    @Deprecated
    void createSentenceHashForExistingEntries();

    @Deprecated
    void regenerateTextConversion(boolean missingOnly);
}
