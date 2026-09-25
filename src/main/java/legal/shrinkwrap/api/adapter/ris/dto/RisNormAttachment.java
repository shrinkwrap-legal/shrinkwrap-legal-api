package legal.shrinkwrap.api.adapter.ris.dto;

import at.gv.bka.ris.v26.soap.ws.client.WebDocumentContentType;
import at.gv.bka.ris.v26.soap.ws.client.WebDocumentDataType;

/**
 * A file belonging to a norm document that is not the main document.
 * <p>
 * Two kinds occur and they behave differently: {@link WebDocumentContentType#EMBEDDED_ATTACHMENT}
 * is an image referenced from within the html (§ 50 StVO carries 37 of them), while
 * {@link WebDocumentContentType#ATTACHMENT} is a file the html does not mention at all - the
 * annexes of the KEM-V are pdf, and their html holds nothing but the note that this is so.
 *
 * @param name RIS name; for an embedded image this is the file name used in the html {@code src}
 */
public record RisNormAttachment(
        WebDocumentContentType kind,
        String name,
        WebDocumentDataType dataType,
        String url
) {
}
