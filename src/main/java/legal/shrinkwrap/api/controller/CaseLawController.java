package legal.shrinkwrap.api.controller;

import jakarta.validation.Valid;
import legal.shrinkwrap.api.adapter.ris.dto.RisCourt;
import legal.shrinkwrap.api.dto.CaseLawRequestDto;
import legal.shrinkwrap.api.dto.CaseLawResponseDto;
import legal.shrinkwrap.api.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.text.MessageFormat;
import java.util.HashSet;

@RestController
@Validated
@Slf4j
public class CaseLawController {

    private static final Logger LOG = LoggerFactory.getLogger(CaseLawController.class);

    private final DocumentService documentService;

    public CaseLawController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping(value = "case-law/shrinkwrap", produces = MediaType.APPLICATION_JSON_VALUE)
    public CaseLawResponseDto getShrinkwrapDocument(@Valid @ParameterObject CaseLawRequestDto requestDto) {
        CaseLawResponseDto document = documentService.getDocument(requestDto);
        return document;
    }

    @GetMapping(value = "case-law/shrinkwrap/{court}/{docNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    public CaseLawResponseDto getShrinkwrapDocumentByCourtAndDocNumber(
            @PathVariable("court") RisCourt court,
            @PathVariable("docNumber") String docNumber,
            @RequestParam(value = "includePrompts", required = false) Boolean includePrompts) {
        //alternative method, having court and docNumber as path variables
        CaseLawRequestDto requestDto = new CaseLawRequestDto(
                null,
                docNumber,
                court,
                includePrompts
        );
        return documentService.getDocument(requestDto);
    }

    @GetMapping("case-law/overview")
    public void getCaselawOverview(@RequestParam("docNumber") String docNumber, @RequestParam("court") String court) {
        LOG.info(MessageFormat.format("court {1}, docNumber {0}", docNumber, court));

    }
}
