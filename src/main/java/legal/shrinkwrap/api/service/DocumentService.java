package legal.shrinkwrap.api.service;

import legal.shrinkwrap.api.adapter.ris.dto.RisJudikaturResult;
import legal.shrinkwrap.api.dataset.CaseLawDataset;
import legal.shrinkwrap.api.dto.CaseLawResponseDto;
import legal.shrinkwrap.api.dto.CaseLawRequestDto;
import legal.shrinkwrap.api.persistence.entity.CaseLawEntity;


public interface DocumentService {

    CaseLawResponseDto getDocument(CaseLawRequestDto caseLawRequestDto);

    CaseLawEntity downloadCaseLaw(CaseLawRequestDto requestDto);

    CaseLawDataset getCaselawDatasetForECLI(String ecli);

    CaseLawEntity importJudikaturResult(RisJudikaturResult result);

    @Deprecated
    void createSentenceHashForExistingEntries();

    @Deprecated
    void regenerateTextConversion(boolean missingOnly);
}
