package legal.shrinkwrap.api.controller;

import jakarta.validation.Valid;
import legal.shrinkwrap.api.dto.CaseLawResponseDto;
import legal.shrinkwrap.api.dto.DocNumberDto;
import legal.shrinkwrap.api.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.MessageFormat;

@RestController
@Validated
public class CaseLawController {

    private static final Logger LOG = LoggerFactory.getLogger(CaseLawController.class);

    private final DocumentService documentService;

    public CaseLawController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("case-law/shrinkwrap")
    public CaseLawResponseDto getShrinkwrapDocument(@RequestParam("docNumber") @Valid @ParameterObject DocNumberDto docNumberDto) {
        LOG.info("DocNumber {}", docNumberDto);

        CaseLawResponseDto document = documentService.getDocument(docNumberDto);
        return document;
    }

    @GetMapping("case-law/overview")
    public void getCaselawOverview(@RequestParam("docNumber") String docNumber, @RequestParam("court") String court) {
        LOG.info(MessageFormat.format("court {1}, docNumber {0}", docNumber, court));

    }
}
