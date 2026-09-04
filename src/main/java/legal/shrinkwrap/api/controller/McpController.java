package legal.shrinkwrap.api.controller;

import legal.shrinkwrap.api.adapter.ris.dto.RisCourt;
import com.google.common.base.Strings;
import legal.shrinkwrap.api.dto.CaseLawFullTextDto;
import legal.shrinkwrap.api.dto.CaseLawMetadataDto;
import legal.shrinkwrap.api.dto.CaseLawResponseDto;
import legal.shrinkwrap.api.dto.CaseLawSearchResponseDto;
import legal.shrinkwrap.api.dto.CaselawSummaryCivilCase;
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
              
                          Results are the 50 most recent decisions that match, newest first
              
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

        ret.getSearchResults().forEach(e -> {
            replaceNulls(e.getSummary());
            replaceNulls(e.getMetadata());
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
        replaceNulls(e.getMetadata());
        return e;
    }

    /**
     * The generated output schema types every property without "null", so a null value makes the
     * SDK's schema validation reject the whole response. The MCP tool serialisation runs through
     * {@link io.modelcontextprotocol.json.McpJsonDefaults}, not through a mapper we could configure
     * to leave nulls out - so the values are replaced here instead.
     * <p>
     * Keep this in sync when a property is added to the DTO.
     */
    static void replaceNulls(CaselawSummaryCivilCase summary) {
        if (summary == null) {
            return;
        }
        summary.setEugh(summary.getEugh() != null && summary.getEugh());
        summary.setArt(Strings.nullToEmpty(summary.getArt()));
        summary.setAusgang(Strings.nullToEmpty(summary.getAusgang()));
        summary.setRechtsmittel(Strings.nullToEmpty(summary.getRechtsmittel()));
        summary.setVerfahrensart(Strings.nullToEmpty(summary.getVerfahrensart()));
        summary.setSachverhalt(Strings.nullToEmpty(summary.getSachverhalt()));
        summary.setBegehren(Strings.nullToEmpty(summary.getBegehren()));
        summary.setGegenvorbringen(Strings.nullToEmpty(summary.getGegenvorbringen()));
        summary.setEntscheidung_gericht(Strings.nullToEmpty(summary.getEntscheidung_gericht()));
        summary.setBerufende_partei(Strings.nullToEmpty(summary.getBerufende_partei()));
        summary.setZusammenfassung_3_saetze(Strings.nullToEmpty(summary.getZusammenfassung_3_saetze()));
        summary.setZeitungstitel_boulevard(Strings.nullToEmpty(summary.getZeitungstitel_boulevard()));
        summary.setZeitungstitel_rechtszeitschrift(Strings.nullToEmpty(summary.getZeitungstitel_rechtszeitschrift()));
        summary.setZeitungstitel_oeffentlich(Strings.nullToEmpty(summary.getZeitungstitel_oeffentlich()));
        summary.setHauptrechtsgebiete(ListUtils.emptyIfNull(summary.getHauptrechtsgebiete()));
        summary.setUnterrechtsgebiete(ListUtils.emptyIfNull(summary.getUnterrechtsgebiete()));
        summary.setSchlussfolgerungen(ListUtils.emptyIfNull(summary.getSchlussfolgerungen()));
        summary.setWichtige_normen(ListUtils.emptyIfNull(summary.getWichtige_normen()));
        summary.setZusammenfassung_3_absaetze(ListUtils.emptyIfNull(summary.getZusammenfassung_3_absaetze()));
    }

    /** @see #replaceNulls(CaselawSummaryCivilCase) - decisionDate has no empty value and stays null. */
    static void replaceNulls(CaseLawMetadataDto metadata) {
        if (metadata == null) {
            return;
        }
        metadata.setOrgan(Strings.nullToEmpty(metadata.getOrgan()));
        metadata.setCourt(Strings.nullToEmpty(metadata.getCourt()));
        metadata.setDecisionType(Strings.nullToEmpty(metadata.getDecisionType()));
        metadata.setUrl(Strings.nullToEmpty(metadata.getUrl()));
        metadata.setEcli(Strings.nullToEmpty(metadata.getEcli()));
        metadata.setCaseNumber(Strings.nullToEmpty(metadata.getCaseNumber()).trim());
    }

}
