package legal.shrinkwrap.api.service;

import at.gv.bka.ris.v26.soap.ws.client.WebDocumentContentType;
import jodd.jerry.Jerry;
import legal.shrinkwrap.api.adapter.ris.dto.RisNormAttachment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.List;

/**
 * Cleans RIS norm HTML (BrKons/LrKons) so that pandoc {@code -t plain} produces usable text.
 * The counterpart to {@link CaselawTextService} for judicature; the contract is the same, the
 * caller runs pandoc on the returned HTML.
 * <p>
 * Norm HTML differs from judicature HTML in three ways that all destroy the text if ignored:
 * <ul>
 *     <li>enumerations are real {@code <ol>}/{@code <ul>} lists, so pandoc renumbers them - a
 *         "3a." becomes "4." in <em>every</em> output format. The scaffolding is therefore
 *         dissolved and the original markers are kept as literal text;</li>
 *     <li>the marker sits in a sibling {@code div.SymE<n>} of the content, so without merging
 *         the two it ends up on its own line and the nesting is lost;</li>
 *     <li>images are referenced root-relative and carry an empty {@code alt}, so pandoc renders
 *         them as an anonymous "[]" - see {@link #absolutizeImages}.</li>
 * </ul>
 * Verified against § 18 EStG (64 markers over three levels) and § 50 StVO (37 images).
 */
@Service
public class NormTextService {

    private static final String NBSP = "\u00a0";

    /** Keeps marker and text apart; pandoc collapses ordinary spaces here, a nbsp survives. */
    private static final String SEPARATOR = NBSP + " ";

    /** Nbsp per nesting level - ordinary leading spaces are stripped by pandoc. */
    private static final int INDENT_PER_LEVEL = 4;

    private final String documentBaseUrl;

    public NormTextService(@Value("${ris.norm.document-base-url}") String documentBaseUrl) {
        this.documentBaseUrl = documentBaseUrl;
    }

    /**
     * Cleans up a RIS norm HTML response.
     *
     * @param uncleanedRisHtml the document as delivered by RIS
     * @param attachments      the document's attachments as RIS reports them. Needed because a
     *                         non-embedded attachment appears nowhere in the html - see
     *                         {@link #appendAttachmentLinks}. Pass an empty list if there are none
     * @return HTML reduced to the provision text, ready for pandoc
     */
    public String prepareRisNormHtml(String uncleanedRisHtml, List<RisNormAttachment> attachments) {
        //same whitespace normalisation as for judicature
        String html = uncleanedRisHtml.replaceAll("(\\u00a0|&nbsp;|&#160;)+", NBSP);
        html = html.replaceAll("(\\u00a0|&nbsp;|&#160;) ", " ");
        html = html.replaceAll(" (\\u00a0|&nbsp;|&#160;)", " ");

        Jerry doc = Jerry.of(html);

        //remove all sr-only content (screenreader only), as the judicature cleanup does
        doc.find(".sr-only").remove();
        doc.find("head").remove();
        doc.find("script").remove();
        doc.find("style").remove();

        keepTextBlockOnly(doc);
        mergeParagraphSymbol(doc);
        mergeListMarkers(doc);
        separateAbsatzzahl(doc);
        absolutizeImages(doc);
        appendAttachmentLinks(doc, attachments);

        return dissolveLists(doc.htmlAll(true));
    }

    /**
     * Keeps only the "Text" block. Everything before it (Kurztitel, Kundmachungsorgan, Index, …)
     * and everything after it (Anmerkung, Schlagworte, Gesetzesnummer, Dokumentnummer) is
     * metadata that is held in columns of its own.
     */
    private static void keepTextBlockOnly(Jerry doc) {
        Iterator<Jerry> blocks = doc.find(".contentBlock").iterator();
        while (blocks.hasNext()) {
            Jerry block = blocks.next();
            Jerry title = block.find("h1.Titel").first();
            if (title.length() == 0 || !"Text".equalsIgnoreCase(title.text().trim())) {
                block.remove();
            } else {
                //the label itself is not part of the provision
                title.remove();
            }
        }
    }

    /**
     * Merges the paragraph symbol into the first text block of its paragraph. RIS puts "§ 1."
     * into an element of its own and renders it into the first line; taken literally the symbol
     * forms a block and the text breaks after it, leaving the number on a line by itself.
     * <p>
     * Two shapes occur. In the EStG the symbol sits in a floated wrapper
     * ({@code div.GldSymbolFloatLeft}), in the Buchpreisbindungsgesetz it is a bare
     * {@code h3.GldSymbol} next to its paragraph. Keying on the symbol rather than on the
     * wrapper covers both.
     * <p>
     * Paragraphs with a heading of their own are not built this way at all - there the symbol is
     * part of the heading ("§ 50. Die Gefahrenzeichen.") and nothing needs merging.
     */
    private static void mergeParagraphSymbol(Jerry doc) {
        Iterator<Jerry> symbols = doc.find(".GldSymbol").iterator();
        while (symbols.hasNext()) {
            Jerry symbol = symbols.next();
            String text = symbol.text().replace(NBSP, " ").trim();
            if (text.isEmpty()) {
                continue;
            }

            //the floated wrapper exists only to hold the symbol and has to go with it
            Jerry removable = symbol;
            if (symbol.parent().hasClass("GldSymbolFloatLeft")) {
                removable = symbol.parent();
            }
            Jerry paragraph = removable.parent();

            //has to go first, the symbol itself would otherwise pass as the first text block
            removable.remove();

            Jerry target = firstTextBlock(paragraph);
            if (target != null) {
                target.html(text + SEPARATOR + target.html());
            } else {
                paragraph.prepend("<div>" + text + "</div>");
            }
        }
    }

    /** The first {@code div}/{@code p} that carries text of its own rather than further blocks. */
    private static Jerry firstTextBlock(Jerry container) {
        for (Jerry candidate : container.find("div, p")) {
            boolean holdsBlocks = false;
            for (Jerry child : candidate.children()) {
                if (isBlockOrList(child)) {
                    holdsBlocks = true;
                    break;
                }
            }
            if (!holdsBlocks) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean isBlockOrList(Jerry element) {
        String tag = element.get(0).getNodeName();
        return isBlock(element) || "ol".equalsIgnoreCase(tag) || "ul".equalsIgnoreCase(tag);
    }

    /**
     * Merges the marker div of a list item into the first block of its content div and
     * materialises the nesting level as indentation. Without this the marker forms a block of
     * its own and "1." and its text end up separated by a blank line.
     */
    private static void mergeListMarkers(Jerry doc) {
        Iterator<Jerry> items = doc.find("li").iterator();
        while (items.hasNext()) {
            Jerry item = items.next();

            Jerry symbol = null;
            Jerry content = null;
            for (Jerry child : item.children()) {
                String cssClass = child.attr("class");
                if (cssClass == null) {
                    continue;
                }
                String firstToken = cssClass.split("\\s+")[0];
                if (firstToken.startsWith("SymE")) {
                    symbol = child;
                } else if (firstToken.equals("content")) {
                    content = child;
                }
            }
            if (symbol == null || content == null) {
                continue;
            }

            String marker = symbol.text().replace(NBSP, " ").trim();
            //level 0 exists as well - state law lists flush with the paragraph text as SymE0 (the NÖ
            //Jagdgesetz 1974 does) - and it is indented no more than level 1
            String indent = NBSP.repeat(INDENT_PER_LEVEL * Math.max(0, nestingLevel(symbol) - 1));
            symbol.remove();

            Jerry firstBlock = content.children().first();
            if (firstBlock.length() > 0 && isBlock(firstBlock)) {
                firstBlock.html(indent + marker + SEPARATOR + firstBlock.html());
            } else {
                //the item carries no text of its own, only a sub-list (e.g. § 18 Abs 1 Z 4 EStG)
                content.prepend("<div>" + indent + marker + "</div>");
            }
        }
    }

    /** {@code SymE0} … {@code SymE3}; anything unexpected counts as the outermost level. */
    private static int nestingLevel(Jerry symbol) {
        String firstToken = symbol.attr("class").split("\\s+")[0];
        char last = firstToken.charAt(firstToken.length() - 1);
        return Character.isDigit(last) ? Character.getNumericValue(last) : 1;
    }

    private static boolean isBlock(Jerry element) {
        String tag = element.get(0).getNodeName();
        return "div".equalsIgnoreCase(tag) || "p".equalsIgnoreCase(tag);
    }

    /** Without a separator the paragraph number runs into the text: "(1)Folgende Ausgaben …". */
    private static void separateAbsatzzahl(Jerry doc) {
        doc.find("span.Absatzzahl").each((span, index) -> {
            span.html(span.html() + SEPARATOR);
            return true;
        });
    }

    /**
     * RIS references images root-relative ({@code /Dokumente/Bundesnormen/NOR…/image001.png})
     * and leaves {@code alt} empty, so pandoc emits a bare "[]" - which claims something is
     * there without saying what or where. The stored HTML is made self-contained instead, and
     * the numbering matches the ordinal of the attachment record.
     */
    private void absolutizeImages(Jerry doc) {
        int[] ordinal = {0};
        doc.find("img").each((image, index) -> {
            String source = image.attr("src");
            if (source == null) {
                return true;
            }
            if (source.startsWith("/")) {
                source = documentBaseUrl + source;
                image.attr("src", source);
            }
            ordinal[0]++;
            String alt = image.attr("alt");
            if (alt == null || alt.isBlank()) {
                image.attr("alt", "Abbildung " + ordinal[0] + ": " + source);
            }
            return true;
        });
    }

    /**
     * Appends a link for every attachment that is not embedded in the text.
     * <p>
     * An embedded image is an {@code <img>} and is dealt with where it stands. A plain attachment
     * is not: the annexes of the KEM-V are pdf files, and their html mentions them with no tag at
     * all - the whole body is the sentence "(Anm.: Anlage ist als PDF dokumentiert.)". Without
     * this step a 237 kB annex is answered with a 40 character stub that reads like real content.
     * <p>
     * The url is repeated inside the link text on purpose: pandoc's plain writer keeps the text
     * and drops the href, so a link whose text is just "Anlage 1" would lose the address.
     */
    private static void appendAttachmentLinks(Jerry doc, List<RisNormAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        StringBuilder links = new StringBuilder();
        for (RisNormAttachment attachment : attachments) {
            if (WebDocumentContentType.EMBEDDED_ATTACHMENT.equals(attachment.kind())) {
                continue;
            }
            String url = escape(attachment.url());
            String label = escape(attachment.name());
            String type = attachment.dataType() == null ? "" : " (" + attachment.dataType().value() + ")";
            links.append("<p><a href=\"").append(url).append("\">[")
                    .append(label).append(type).append(": ").append(url).append("]</a></p>");
        }
        if (links.isEmpty()) {
            return;
        }
        Jerry target = doc.find(".contentBlock").first();
        if (target.length() == 0) {
            target = doc.find("body").first();
        }
        if (target.length() > 0) {
            target.append(links.toString());
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /**
     * Removes the list scaffolding so pandoc cannot renumber anything; the markers are plain
     * text by now. This is a flat tag substitution and deliberately not a structural match -
     * the nesting has already been dealt with on the DOM.
     */
    private static String dissolveLists(String html) {
        return html.replaceAll("</?(ol|ul)\\b[^>]*>", "")
                .replaceAll("<li\\b[^>]*>", "<div>")
                .replace("</li>", "</div>");
    }
}
