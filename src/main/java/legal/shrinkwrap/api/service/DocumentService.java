package legal.shrinkwrap.api.service;

import legal.shrinkwrap.api.dto.CaseLawResponseDto;
import legal.shrinkwrap.api.dto.DocNumberDto;


public interface DocumentService {

    CaseLawResponseDto getDocument(DocNumberDto docNumberDto);

}
