package legal.shrinkwrap.api.service;

import legal.shrinkwrap.api.adapter.ris.dto.RisJudikaturResult;
import legal.shrinkwrap.api.dataset.CaseLawDataset;
import legal.shrinkwrap.api.dto.CaseLawResponseDto;
import legal.shrinkwrap.api.dto.CaseLawRequestDto;

import java.time.Year;


public interface DocumentService {

    CaseLawResponseDto getDocument(CaseLawRequestDto caseLawRequestDto);

    CaseLawDataset getCaselawDatasetForECLI(String ecli);

    void importJudikaturResult(RisJudikaturResult result);
}
