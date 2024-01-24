package legal.shrinkwrap.api.service;

import legal.shrinkwrap.api.dto.DocNumberDto;
import org.springframework.stereotype.Service;


public interface DocumentService {

    void getDocument(DocNumberDto docNumberDto);


}
