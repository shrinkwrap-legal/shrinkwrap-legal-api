package legal.shrinkwrap.api.service;

import legal.shrinkwrap.api.adapter.HtmlDownloadService;
import legal.shrinkwrap.api.adapter.ris.rest.RisAdapter;
import legal.shrinkwrap.api.adapter.ris.rest.dto.enums.OgdApplikationEnum;
import legal.shrinkwrap.api.dto.CaseLawResponseDto;
import legal.shrinkwrap.api.dto.DocNumberDto;
import org.springframework.stereotype.Service;

@Service
public class DocumentServiceImpl implements DocumentService {

    private final RisAdapter risAdapter;

    private final HtmlDownloadService htmlDownloadService;

    private final CaselawTextService caselawTextService;

    public DocumentServiceImpl(RisAdapter risAdapter, HtmlDownloadService htmlDownloadService, CaselawTextService caselawTextService) {
        this.risAdapter = risAdapter;
        this.htmlDownloadService = htmlDownloadService;
        this.caselawTextService = caselawTextService;
    }

    @Override
    public CaseLawResponseDto getDocument(DocNumberDto docNumberDto) {
        //@TODO: Map to OgdApplikationEnum ?
        String justiz = risAdapter.getCaselawByDocNumberAsHtml(OgdApplikationEnum.Justiz, docNumberDto.docNumber());
        CaseLawResponseDto res = caselawTextService.prepareRISCaseLawHtml(justiz);
        return res;
    }
}