package legal.shrinkwrap.api.controller;

import jakarta.validation.Valid;
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

    @GetMapping("case-law/overview")
    public void getCaselawOverview(@RequestParam("docNumber") String docNumber, @RequestParam("court") String court) {
        LOG.info(MessageFormat.format("court {1}, docNumber {0}", docNumber, court));

    }
}
