package legal.shrinkwrap.api.service;

import legal.shrinkwrap.api.adapter.ris.dto.RisJudikaturResult;
import legal.shrinkwrap.api.dataset.CaseLawDataset;
import legal.shrinkwrap.api.dto.CaseLawResponseDto;
import legal.shrinkwrap.api.dto.CaseLawRequestDto;
import legal.shrinkwrap.api.persistence.entity.CaseLawEntity;

import java.time.Year;


public interface DocumentService {

    CaseLawResponseDto getDocument(CaseLawRequestDto caseLawRequestDto);

    CaseLawEntity downloadCaseLaw(CaseLawRequestDto requestDto);

    CaseLawDataset getCaselawDatasetForECLI(String ecli);

    void importJudikaturResult(RisJudikaturResult result);
}
