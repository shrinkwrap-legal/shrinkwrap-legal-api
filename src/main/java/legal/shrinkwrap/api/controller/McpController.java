package legal.shrinkwrap.api.controller;

import legal.shrinkwrap.api.adapter.ris.dto.RisCourt;
import com.google.common.base.Strings;
import legal.shrinkwrap.api.dto.CaseLawFullTextDto;
import legal.shrinkwrap.api.dto.CaseLawMetadataDto;
import legal.shrinkwrap.api.dto.CaseLawResponseDto;
import legal.shrinkwrap.api.dto.CaseLawSearchResponseDto;
import legal.shrinkwrap.api.dto.CaselawSummaryCivilCase;
import legal.shrinkwrap.api.dto.NormLawDto;
import legal.shrinkwrap.api.dto.NormLawsDto;
import legal.shrinkwrap.api.dto.NormProvisionsDto;
import legal.shrinkwrap.api.service.DocumentService;
import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpMeta;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class McpController {
    private static final Logger LOG = LoggerFactory.getLogger(McpController.class);

    /** As many laws as the REST default returns; the total says whether there were more. */
    private static final int NORM_LAWS_LIMIT = 20;

    private final DocumentService documentService;

    /**
     * The norm tools answer through the REST handlers rather than beside them: resolving a name,
     * refusing an ambiguous one and the whole handling of time live there and must not exist twice.
     */
    private final NormController normController;

    public McpController(DocumentService documentService, NormController normController) {
        this.documentService = documentService;
        this.normController = normController;
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

    @McpTool(name = "find_austrian_law",
            description = """
            Find an Austrian law by its abbreviation or its title, as it is written in a citation.

            Accepts the abbreviation with or without its year ("StVO", "StVO 1960", "stvo1960") and the short
            title or a part of it ("Buchpreisbindungsgesetz", "Telekommunikationsgesetz"). Hyphens matter:
            "B-VG" is not "BVG". A name that carries a year is looked up with the year first, so "TKG 2003"
            finds only that law while "TKG" finds every TKG.

            Returns the gesetzesnummer, the unambiguous way to address the law in
            retrieve_austrian_norm_provisions, together with titles, type (BG federal act, BVG constitutional
            act, V regulation, LG state act, ...) and the Kundmachungsorgan, which names the original
            publication and the last amendment. Laws that are no longer in force are included; the
            Kundmachungsorgan then reads "... aufgehoben durch ...".

            Recommended agent workflow:
            1. find_austrian_law to learn the gesetzesnummer - unless the abbreviation is unambiguous anyway.
            2. retrieve_austrian_norm_provisions without text to see which provisions exist and how long they are.
            3. retrieve_austrian_norm_provisions with includeText for the provisions that matter.
            """,
            annotations = @McpTool.McpAnnotations(
                    title = "Find an Austrian law",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true
            ),
            generateOutputSchema = true)
    public NormLawsDto findAustrianLaw(
            @McpToolParam(description = """
                    Where the law applies, ISO 3166-2: AT for federal law, AT-1 Burgenland, AT-2 Kärnten,
                    AT-3 Niederösterreich, AT-4 Oberösterreich, AT-5 Salzburg, AT-6 Steiermark, AT-7 Tirol,
                    AT-8 Vorarlberg, AT-9 Wien. Required: "BauO" exists in every state.
                    """) String jurisdiction,
            @McpToolParam(description = "Abbreviation or title of the law, e.g. \"TKG 2021\", \"ABGB\", \"Kesselgesetz\"")
            String query) {
        ResponseEntity<List<NormLawDto>> found = normController.findLaws(jurisdiction, query, NORM_LAWS_LIMIT);
        return new NormLawsDto(Integer.parseInt(found.getHeaders().getFirst(NormController.TOTAL_COUNT)),
                found.getBody());
    }

    @McpTool(name = "retrieve_austrian_norm_provisions",
            description = """
            Retrieve provisions (§§, articles, annexes) of an Austrian law, with or without their text.

            Address the law by gesetzesnummer (from find_austrian_law) or by abbreviation as cited ("ABGB",
            "TKG 2021"). An abbreviation that names several laws is narrowed to those with provisions in the
            requested time; if still several remain, the call fails with the candidates and their
            gesetzesnummer - repeat it with the one that is meant. Alternatively pass risUrl values from an
            earlier answer to fetch exactly those versions; then no law needs to be named.

            Time: by default the law as it stands today. asOf moves that to another day. inForceFrom and/or
            inForceUntil return every version in force at any point within that stretch - the way to see what
            changed, e.g. "what changed in the TKG over the last 10 years". A repealed law has no provisions
            today; ask for a date when it was in force.

            Range: from and to take a designation - "12", "12a", "§ 12a", "Art. 4", "Anlage 3". A single
            provision is from without to. A range includes everything in between, so 12a to 14c also brings
            up 13 and 13a; a letter follows its number, so from=1a starts after § 1.

            Each provision reports its versions' validity, the original publication (stammfassung), the
            publication that last touched it (letzteAenderung) and wordCount. Retrieve the list without text
            first and fetch text only where needed - a large law runs to thousands of provisions. total above
            the number returned means the list was cut off at limit.
            """,
            annotations = @McpTool.McpAnnotations(
                    title = "Get provisions of an Austrian law",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true
            ),
            generateOutputSchema = true)
    public NormProvisionsDto retrieveAustrianNormProvisions(
            @McpToolParam(required = false, description = "AT for federal law, AT-1 … AT-9 for the states (see find_austrian_law). Required unless risUrl is given")
            String jurisdiction,
            @McpToolParam(required = false, description = "RIS law number from find_austrian_law, e.g. \"20011678\" for the TKG 2021")
            String gesetzesnummer,
            @McpToolParam(required = false, description = "Abbreviation as cited, e.g. \"ABGB\" - alternative to gesetzesnummer")
            String abbreviation,
            @McpToolParam(required = false, description = "One or several risUrl values from an earlier answer, separated by commas; fetches exactly those versions")
            String risUrl,
            @McpToolParam(required = false, description = "First provision, e.g. \"§ 85\", \"Art. 4\", \"12a\"; alone it asks for a single provision")
            String from,
            @McpToolParam(required = false, description = "Last provision of a range, e.g. \"§ 90\"")
            String to,
            @McpToolParam(required = false, description = "The law as it stood on this day, ISO-8601 YYYY-MM-DD; default today")
            LocalDate asOf,
            @McpToolParam(required = false, description = "Every version in force at some point on or after this day, ISO-8601; combine with inForceUntil")
            LocalDate inForceFrom,
            @McpToolParam(required = false, description = "Every version in force at some point up to this day, ISO-8601; combine with inForceFrom")
            LocalDate inForceUntil,
            @McpToolParam(required = false, description = "Include the full text; default false. Check wordCount first")
            Boolean includeText,
            @McpToolParam(required = false, description = "Maximum number of provisions; default 25, at most 2000")
            Integer limit) {
        List<String> risUrls = risUrl == null ? null : Arrays.stream(risUrl.split(","))
                .map(String::trim)
                .filter(url -> !url.isEmpty())
                .toList();
        return normController.getProvisions(jurisdiction, gesetzesnummer, abbreviation, risUrls, null,
                asOf, inForceFrom, inForceUntil, from, to, null,
                includeText != null && includeText, limit == null ? 25 : limit).getBody();
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
