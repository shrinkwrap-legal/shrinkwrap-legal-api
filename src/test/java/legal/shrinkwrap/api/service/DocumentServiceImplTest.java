package legal.shrinkwrap.api.service;

import legal.shrinkwrap.api.dto.CaseLawResponseDto;
import legal.shrinkwrap.api.dto.CaseLawRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DocumentServiceImplTest {

    @Autowired
    private DocumentService documentService;

    @Test
    void getDocument() {
        String docNumber="JJT_20240131_OLG0009_0320BS00233_23H0000_000";
        //CaseLawRequestDto dto = new CaseLawRequestDto(docNumber, "Justiz");
        //CaseLawResponseDto document = documentService.getDocument(dto);
    }
}