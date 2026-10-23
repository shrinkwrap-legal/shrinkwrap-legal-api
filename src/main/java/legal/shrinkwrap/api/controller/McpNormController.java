package legal.shrinkwrap.api.controller;

import legal.shrinkwrap.api.dto.NormLawDto;
import legal.shrinkwrap.api.dto.NormLawsDto;
import legal.shrinkwrap.api.dto.NormProvisionsDto;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/** The norm tools; the case law tools are in {@link McpController}, so each can be switched off. */
@Controller
@ConditionalOnProperty(name = "enable-norms", havingValue = "true", matchIfMissing = true)
public class McpNormController {

    /** As many laws as the REST default returns; the total says whether there were more. */
    private static final int NORM_LAWS_LIMIT = 20;

    /**
     * The norm tools answer through the REST handlers rather than beside them: resolving a name,
     * refusing an ambiguous one and the whole handling of time live there and must not exist twice.
     */
    private final NormController normController;

    public McpNormController(NormController normController) {
        this.normController = normController;
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
}
