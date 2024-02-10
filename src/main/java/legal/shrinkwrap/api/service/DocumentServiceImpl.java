package legal.shrinkwrap.api.service;

import legal.shrinkwrap.api.adapter.HtmlDownloadService;
import legal.shrinkwrap.api.adapter.ris.RisAdapter;
import legal.shrinkwrap.api.adapter.ris.dto.OgdSearchResult;
import legal.shrinkwrap.api.adapter.ris.dto.enums.OgdApplikationEnum;
import legal.shrinkwrap.api.dto.CaseLawResponseDto;
import legal.shrinkwrap.api.dto.DocNumberDto;
import org.springframework.stereotype.Service;

@Service
public class DocumentServiceImpl implements DocumentService {

    private final RisAdapter risAdapter;

    private final HtmlDownloadService htmlDownloadService;

    public DocumentServiceImpl(RisAdapter risAdapter, HtmlDownloadService htmlDownloadService) {
        this.risAdapter = risAdapter;
        this.htmlDownloadService = htmlDownloadService;
    }

    @Override
    public CaseLawResponseDto getDocument(DocNumberDto docNumberDto) {
        //@TODO: Map to OgdApplikationEnum ?
        OgdSearchResult justiz = risAdapter.getJustiz(OgdApplikationEnum.Justiz, docNumberDto.docNumber());

        return null;
    }
}