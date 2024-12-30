package legal.shrinkwrap.api.service;

import legal.shrinkwrap.api.adapter.HtmlDownloadService;
import legal.shrinkwrap.api.adapter.ris.RisSearchParameterCaseLaw;
import legal.shrinkwrap.api.adapter.ris.RisSoapAdapter;
import legal.shrinkwrap.api.adapter.ris.dto.RisCourt;
import legal.shrinkwrap.api.adapter.ris.dto.RisJudikaturResult;
import legal.shrinkwrap.api.adapter.ris.dto.RisSearchResult;
import legal.shrinkwrap.api.dto.CaseLawRequestDto;
import legal.shrinkwrap.api.dto.CaseLawResponseDto;
import org.springframework.stereotype.Service;

@Service
public class DocumentServiceImpl implements DocumentService {

    private final RisSoapAdapter risSoapAdapter;

    private final HtmlDownloadService htmlDownloadService;

    private final CaselawTextService caselawTextService;

    public DocumentServiceImpl(RisSoapAdapter risSoapAdapter, HtmlDownloadService htmlDownloadService, CaselawTextService caselawTextService) {
        this.risSoapAdapter = risSoapAdapter;
        this.htmlDownloadService = htmlDownloadService;
        this.caselawTextService = caselawTextService;
    }

    @Override
    public CaseLawResponseDto getDocument(CaseLawRequestDto requestDto) {

        RisSearchResult result = risSoapAdapter.findCaseLawDocuments(RisSearchParameterCaseLaw.builder()
                .court(RisCourt.Justiz)
                .ecli(requestDto.ecli()).build()
                );
        if(result == null) {
            return null;
        }
        RisJudikaturResult justiz = result.getJudikaturResults().getFirst();

        String htmlContent = htmlDownloadService.downloadHtml(justiz.getHtmlDocumentUrl());

        CaseLawResponseDto res = caselawTextService.prepareRISCaseLawHtml(htmlContent);
        return res;
    }
}