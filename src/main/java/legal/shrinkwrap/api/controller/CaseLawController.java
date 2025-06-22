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
    private final HashSet<CaseLawRequestDto> locks = new HashSet<>();

    private final DocumentService documentService;

    public CaseLawController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping(value = "case-law/shrinkwrap", produces = MediaType.APPLICATION_JSON_VALUE)
    public CaseLawResponseDto getShrinkwrapDocument(@Valid @ParameterObject CaseLawRequestDto requestDto) {

        if (locks.contains(requestDto)) {
            try {
                //try for 40s, then do it anyway
                for (int i=0;i<10*40;i++) {
                    Thread.sleep(100);
                    if (!locks.contains(requestDto)) {
                        break;
                    }
                    LOG.info("trying to acquire lock " + i + " for " + requestDto);
                }

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else {
            locks.add(requestDto);
        }
        try {
            CaseLawResponseDto document = documentService.getDocument(requestDto);
            return document;
        } finally {
            locks.remove(requestDto);
        }

    }

    @GetMapping("case-law/overview")
    public void getCaselawOverview(@RequestParam("docNumber") String docNumber, @RequestParam("court") String court) {
        LOG.info(MessageFormat.format("court {1}, docNumber {0}", docNumber, court));

    }
}
