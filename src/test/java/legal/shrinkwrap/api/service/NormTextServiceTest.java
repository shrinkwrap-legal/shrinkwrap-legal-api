package legal.shrinkwrap.api.service;

import at.gv.bka.ris.v26.soap.ws.client.WebDocumentContentType;
import at.gv.bka.ris.v26.soap.ws.client.WebDocumentDataType;
import legal.shrinkwrap.api.adapter.ris.dto.RisNormAttachment;
import legal.shrinkwrap.api.utils.PandocTextWrapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fixtures are unmodified RIS responses:
 * <ul>
 *     <li>{@code NOR40018832} - § 18 EStG, 64 enumeration markers over three nesting levels</li>
 *     <li>{@code NOR40081328} - § 50 StVO, 37 images</li>
 *     <li>{@code NOR40108380} - KEM-V 2009 Anl. 1, substance is a PDF, the text is a note</li>
 * </ul>
 */
public class NormTextServiceTest {

    private static final String NBSP = "\u00a0";

    private final NormTextService normTextService = new NormTextService("https://ris.bka.gv.at");

    /** The pdf annex of the KEM-V, exactly as RIS reports it in the document's metadata. */
    private static final RisNormAttachment KEMV_ANNEX = new RisNormAttachment(
            WebDocumentContentType.ATTACHMENT, "Anlage 1", WebDocumentDataType.PDF,
            "https://ogd.ris.bka.gv.at/Dokumente/Bundesnormen/NOR40108380/BGBl__II_Nr__212_2009__Anlage_1.pdf");

    private String clean(String docNumber, RisNormAttachment... attachments) throws IOException {
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("files/ris/" + docNumber + ".html")) {
            assertThat(in).as("fixture " + docNumber).isNotNull();
            return normTextService.prepareRisNormHtml(
                    new String(in.readAllBytes(), StandardCharsets.UTF_8), List.of(attachments));
        }
    }

    /**
     * Jerry escapes U+00A0 back to {@code &nbsp;} when it serialises, so the indentation and the
     * separators only become comparable once the entity is decoded again. Pandoc decodes it too.
     */
    private String cleanDecoded(String docNumber) throws IOException {
        return clean(docNumber).replace("&nbsp;", NBSP).replace("&#160;", NBSP);
    }

    @Test
    public void listScaffoldingIsRemoved() throws IOException {
        String html = clean("NOR40018832");

        //pandoc renumbers every real list, so none may be left
        assertThat(html).doesNotContain("<ol", "<ul", "<li");
    }

    @Test
    public void markersKeepTheirTextAndNestingLevel() throws IOException {
        String html = cleanDecoded("NOR40018832");

        //level 1 is flush, every further level adds four nbsp
        assertThat(html).contains("1." + NBSP + " ");
        assertThat(html).contains(NBSP.repeat(4) + "a)" + NBSP + " ");
        assertThat(html).contains(NBSP.repeat(8) + "aa)" + NBSP + " ");
        assertThat(html).contains(NBSP.repeat(8) + "bb)" + NBSP + " ");
    }

    /**
     * State law knows a level 0 as well - dashes flush with the paragraph text, SymE0 - and it
     * used to compute a negative indentation that failed the whole answer. § 7 NÖ Jagdgesetz 1974.
     */
    @Test
    public void levelZeroIsFlushLikeLevelOne() throws IOException {
        String html = cleanDecoded("LNO40035225");

        assertThat(html).contains(">-" + NBSP + " der Erholung oder");
        assertThat(html).doesNotContain(NBSP + "-" + NBSP + " der Erholung oder");
        assertThat(html).doesNotContain("<ul", "<li");
    }

    @Test
    public void paragraphNumberIsSeparatedFromItsText() throws IOException {
        String html = cleanDecoded("NOR40018832");

        //without the separator this reads "(1)Folgende Ausgaben"
        assertThat(html).contains("(1)" + NBSP + " ");
    }

    @Test
    public void paragraphSymbolIsMergedIntoTheFirstParagraph() throws IOException {
        String html = cleanDecoded("NOR40018832");

        //RIS floats "§ 18." into the first line; left as a block it breaks the text after it
        assertThat(html).doesNotContain("GldSymbolFloatLeft");
        assertThat(html).contains("§ 18." + NBSP + " ");
    }

    @Test
    public void metadataBlocksAreDropped() throws IOException {
        String html = clean("NOR40018832");

        assertThat(html).doesNotContain("Kundmachungsorgan", "Dokumentnummer", "Schlagworte");
        assertThat(html).contains("Sonderausgaben");
    }

    @Test
    public void imagesBecomeAbsoluteAndCarryAName() throws IOException {
        String html = clean("NOR40081328");

        //RIS delivers src="/Dokumente/..." - useless once the html is stored
        assertThat(html).doesNotContain("src=\"/");

        Matcher images = Pattern.compile("<img[^>]*>").matcher(html);
        int count = 0;
        while (images.find()) {
            count++;
            String tag = images.group();
            assertThat(tag).contains("src=\"https://ris.bka.gv.at/Dokumente/Bundesnormen/NOR40081328/");
            assertThat(tag).contains("alt=\"Abbildung " + count + ": https://ris.bka.gv.at/");
        }
        assertThat(count).isEqualTo(37);
    }

    /**
     * The html of this annex carries no reference to the pdf at all - no img, no href, only the
     * note. Without the attachment list a 237 kB annex would be answered with a 40 character
     * stub that reads like real content.
     */
    @Test
    public void annexDocumentedAsPdfGetsALink() throws IOException {
        String html = clean("NOR40108380", KEMV_ANNEX);

        assertThat(html).contains("Anlage ist als PDF dokumentiert");
        assertThat(html).contains("href=\"" + KEMV_ANNEX.url() + "\"");
        //the url is repeated as link text, otherwise pandoc's plain writer drops it
        assertThat(html).contains("[Anlage 1 (Pdf): " + KEMV_ANNEX.url() + "]");
    }

    @Test
    public void withoutAttachmentsNothingIsAppended() throws IOException {
        assertThat(clean("NOR40108380")).doesNotContain("<a href");
    }

    /**
     * The contract only holds end to end: the markers have to survive pandoc as literal text.
     * "aa)" is the counter-check - no pandoc list renderer can produce it.
     */
    @Test
    @Tag("integration")
    public void markersAndImagesSurvivePandoc() throws IOException {
        String text = PandocTextWrapper.convertHtmlToText(clean("NOR40018832"));

        //the symbol, the paragraph number and the text belong on one line, as in RIS
        assertThat(text).contains("§ 18." + NBSP + " (1)" + NBSP + " Folgende Ausgaben");
        assertThat(text).contains(NBSP.repeat(8) + "aa)" + NBSP + " ");
        assertThat(text).contains(NBSP.repeat(8) + "bb)" + NBSP + " ");
        assertThat(text).contains(NBSP.repeat(4) + "–" + NBSP + " ");

        //a plain attachment has to survive as a usable address, not as a bare label
        String annex = PandocTextWrapper.convertHtmlToText(clean("NOR40108380", KEMV_ANNEX));
        assertThat(annex).contains("[Anlage 1 (Pdf): " + KEMV_ANNEX.url() + "]");

        String signs = PandocTextWrapper.convertHtmlToText(clean("NOR40081328"));

        //an image without alt text would end up here as a bare "[]"
        assertThat(signs).doesNotContain("[]");
        for (int ordinal = 1; ordinal <= 37; ordinal++) {
            assertThat(signs).contains("[Abbildung " + ordinal + ": https://ris.bka.gv.at/");
        }
    }
}
