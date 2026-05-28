package legal.shrinkwrap.api.controller;

import legal.shrinkwrap.api.adapter.ris.dto.RisCourt;
import legal.shrinkwrap.api.dto.CaseLawRequestDto;
import legal.shrinkwrap.api.dto.CaseLawResponseDto;
import legal.shrinkwrap.api.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpMeta;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class McpController {
    private static final Logger LOG = LoggerFactory.getLogger(McpController.class);

    private final DocumentService documentService;

    public McpController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @McpTool(description = "Make a full-text search thorugh austrian judicature. This function will return some metadata on found cases, as well as an AI generated summary of the case. The full text of the case will be returned if requested - as it can be quite long, the word" +
            " count of the full text will also be returned.")
    public CaseLawResponseDto get(
            @McpToolParam(description = "The court which should be searched. For civil cases, use 'Justiz' which includes the OGH, OLG, LG and Bezirksgerichte. Other courts are the high constitutional court (VfGH), the high administrative court (VwGH), and lower " +
                    "administrativ courts (BVwG for cases which fall into Austrias federal competences i.e. asylum, and LVwG for cases which fall into the competences of the states), and the data protection authority ") RisCourt court,
            @McpToolParam (description = "The full text search query. It is using Postgresql ts_vector for the search, and will pass the query to the websearch_to_tsquery postgres function") String searchQuery,
            McpMeta meta) {
        //alternative method, having court and docNumber as path variables
        //CaseLawRequestDto requestDto = new CaseLawRequestDto(
       return null;
    }

    /*@McpTool
    public CaseLawResponseDto get(
            @McpToolParam(description = "The case number of the case. For example, 6Ob237/21b, 14Ns18/26x, 4R34/26w, W268 2309713-1, 89/01/0061, LVwG-2025/19/1696-5. For VwGH ") String docNumber,
            @RequestParam(value = "includePrompts", required = false) Boolean includePrompts) {
        //alternative method, having court and docNumber as path variables
        CaseLawRequestDto requestDto = new CaseLawRequestDto(
                null,
                docNumber,
                null,
                null,
                true,
                includePrompts
        );
        return documentService.getDocument(requestDto);
    }*/
}
