package legal.shrinkwrap.api.controller;

import com.google.common.base.Strings;
import legal.shrinkwrap.api.adapter.ris.dto.RisCourt;
import legal.shrinkwrap.api.dto.CaseLawFullTextDto;
import legal.shrinkwrap.api.dto.CaseLawMetadataDto;
import legal.shrinkwrap.api.dto.CaseLawResponseDto;
import legal.shrinkwrap.api.dto.CaseLawSearchResponseDto;
import legal.shrinkwrap.api.service.DocumentService;
import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpMeta;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class McpController {
    private static final Logger LOG = LoggerFactory.getLogger(McpController.class);

    private final DocumentService documentService;

    public McpController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @McpTool(name = "search_austrian_case_law",
            description = """
                          Search Austrian case law using full-text search.
              
                          Returns matching cases with metadata, an AI-generated summary, and the full-text word count.
                          Does not return the full text itself. To retrieve the full text, call getCaseLawFullTextByEcli
                          with the ECLI from a search result.
                          The AI summary may misrepresent the judgment, so it should be verified against the full text. But
                          it can serve as a first indication on the relevance of the case.
              
                          Recommended agent workflow:
                          1. Start with specific German legal terms where possible.
                          2. Search recent decisions first, for example the last 10 years.
                          3. If no relevant results are found, broaden the search query or expand the date range.
                          4. Retrieve full text only for cases that appear relevant from their metadata and summary.
                          """,
            annotations = @McpTool.McpAnnotations(
                    title = "Search for relevant Austrian Judicature",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true
            ),
            generateOutputSchema = true)
    public CaseLawSearchResponseDto searchAustrianCaseLaw(
            @McpToolParam(description = "The 'RIS' application which should be searched. For civil cases, use 'Justiz' which includes the OGH, OLG, LG and Bezirksgerichte. Other courts are the high constitutional court (VfGH), the high administrative court (VwGH), and lower " +
                    "administrativ courts (BVwG for cases which fall into Austria's federal competences i.e. asylum, and LVwG for cases which fall into the competences of the states), and the data protection authority ") RisCourt court,
            @McpToolParam (description = """
                    Full-text search query.
                    
                    The query is passed to PostgreSQL websearch_to_tsquery, so you may combine search terms
                    Prefer concise German legal terms, party-neutral descriptions, statutes, legal concepts,
                    or distinctive phrases. Avoid overly long natural-language questions.
                    """
            ) String searchQuery,
            @McpToolParam(description = """
                    The earliest decision date that should be included.
                    It is recommended to start looking with later decisions and only expand the search if no results are found. 
                    A good initial timespan may be 10 years from the current date. 
                    Use ISO-8601 format: YYYY-MM-DD
                    """) LocalDate earliestDecisionDate,
            @McpToolParam(description = """
                    The latest decision date that should be included. 
                    It is recommended to start looking with later decisions and only expand the search if no results are found. 
                    A good initial timespan may be 10 years from the current date. 
                    Use ISO-8601 format: YYYY-MM-DD
                    """) LocalDate latestDecisionDate,
            McpMeta meta) {
        //alternative method, having court and docNumber as path variables

        CaseLawSearchResponseDto ret = new CaseLawSearchResponseDto();
        List<CaseLawResponseDto> caseLaw = documentService.findCaseLaw(searchQuery, court, earliestDecisionDate, latestDecisionDate);
        ret.setSearchResults(caseLaw.stream().map((e) -> {
            return new CaseLawSearchResponseDto.CaseLawSearchResultDto(e.getWordCount(), e.getSummary(), e.getMetadata());
        }).collect(Collectors.toList()));

        //schema validation needs every "null" to be an empty string.
        ret.getSearchResults().forEach((e) -> {
            if (e.getSummary() != null) {
                e.getSummary().setEugh(e.getSummary().getEugh() != null && e.getSummary().getEugh());
                e.getSummary().setArt(Strings.nullToEmpty(e.getSummary().getArt()));
                e.getSummary().setAusgang(Strings.nullToEmpty(e.getSummary().getAusgang()));
                e.getSummary().setRechtsmittel(Strings.nullToEmpty(e.getSummary().getRechtsmittel()));
                e.getSummary().setVerfahrensart(Strings.nullToEmpty(e.getSummary().getVerfahrensart()));
                e.getSummary().setSachverhalt(Strings.nullToEmpty(e.getSummary().getSachverhalt()));
                e.getSummary().setBegehren(Strings.nullToEmpty(e.getSummary().getBegehren()));
                e.getSummary().setGegenvorbringen(Strings.nullToEmpty(e.getSummary().getGegenvorbringen()));
                e.getSummary().setEntscheidung_gericht(Strings.nullToEmpty(e.getSummary().getEntscheidung_gericht()));
                e.getSummary().setBerufende_partei(Strings.nullToEmpty(e.getSummary().getBerufende_partei()));
                e.getSummary().setZusammenfassung_3_saetze(Strings.nullToEmpty(e.getSummary().getZusammenfassung_3_saetze()));
                e.getSummary().setZeitungstitel_boulevard(Strings.nullToEmpty(e.getSummary().getZeitungstitel_boulevard()));
                e.getSummary().setZeitungstitel_rechtszeitschrift(Strings.nullToEmpty(e.getSummary().getZeitungstitel_rechtszeitschrift()));
                e.getSummary().setZeitungstitel_oeffentlich(Strings.nullToEmpty(e.getSummary().getZeitungstitel_oeffentlich()));
                e.getSummary().setHauptrechtsgebiete(ListUtils.emptyIfNull(e.getSummary().getHauptrechtsgebiete()));
                e.getSummary().setUnterrechtsgebiete(ListUtils.emptyIfNull(e.getSummary().getUnterrechtsgebiete()));
                e.getSummary().setSchlussfolgerungen(ListUtils.emptyIfNull(e.getSummary().getSchlussfolgerungen()));
                e.getSummary().setWichtige_normen(ListUtils.emptyIfNull(e.getSummary().getWichtige_normen()));
                e.getSummary().setZusammenfassung_3_absaetze(ListUtils.emptyIfNull(e.getSummary().getZusammenfassung_3_absaetze()));
            }
            if (e.getMetadata() != null) {
                e.getMetadata().setOrgan(Strings.nullToEmpty(e.getMetadata().getOrgan()));
                e.getMetadata().setCourt(Strings.nullToEmpty(e.getMetadata().getCourt()));
                e.getMetadata().setCaseNumber(Strings.nullToEmpty(e.getMetadata().getCaseNumber()).trim());
                e.getMetadata().setDecisionType(Strings.nullToEmpty(e.getMetadata().getDecisionType()));
                e.getMetadata().setEcli(Strings.nullToEmpty(e.getMetadata().getEcli()));

            }
        });

        return ret;
    }

    @McpTool(name = "retrieve_case_law_full_text_by_ecli",
            description = """
            Retrieve the full text of a single Austrian case-law decision by ECLI.

            Use this after searchAustrianCaseLaw has returned a relevant case.
            The returned text may be long as indicated in the wordCount property of the caseLaw Metadata.
            """,
            annotations = @McpTool.McpAnnotations(
                    title = "Get the text representation fo a single judgement",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true
            ),
    generateOutputSchema = true)
    public CaseLawFullTextDto getCaseLawFullTextByEcli(@McpToolParam(description = "The European Case-Law Identifier of the relevant case") String ecli) {

        CaseLawFullTextDto e = documentService.getFullTextForEcli(ecli);
        if (e == null) {
            return null;
        }
        if (e.getMetadata() != null) {
            e.getMetadata().setOrgan(Strings.nullToEmpty(e.getMetadata().getOrgan()));
            e.getMetadata().setCourt(Strings.nullToEmpty(e.getMetadata().getCourt()));
            e.getMetadata().setCaseNumber(Strings.nullToEmpty(e.getMetadata().getCaseNumber()));
            e.getMetadata().setDecisionType(Strings.nullToEmpty(e.getMetadata().getDecisionType()));
            e.getMetadata().setCaseNumber(Strings.nullToEmpty(e.getMetadata().getCaseNumber()).trim());
            e.getMetadata().setEcli(Strings.nullToEmpty(e.getMetadata().getEcli()));

        }
        return e;
    }

}
