package legal.shrinkwrap.api.service;

import jodd.jerry.Jerry;
import legal.shrinkwrap.api.dto.CaseLawResponseDto;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CaselawTextService {


    @Deprecated
    public String extractContent(String uncleanedRisHTML) {
        //be careful, this will not work with some docs!

        Jerry fullHtml = Jerry.of(uncleanedRisHTML);

        //remove all sr-only content (screenreader only)
        fullHtml.find(".sr-only").remove();
        fullHtml.find("head").remove();

        //paperw can be multiple (annotated with "nextpage")
        Iterator<Jerry> pwIterator = fullHtml.find(".paperw>.contentBlock").iterator();

        return fullHtml.html();

    }

    /**
     * Clean up RIS html response
     * @param uncleanedRisHTML
     * @return
     */
    public String prepareRISCaseLawHtml(String uncleanedRisHTML) {
        //some initial HTML preprocessing, e.g. removing double nbsp;
        uncleanedRisHTML = uncleanedRisHTML.replaceAll("(\\u00a0|&nbsp;|&#160;)+", "\u00a0");

        //clean nbsp followed by regular space, and vice versa
        uncleanedRisHTML = uncleanedRisHTML.replaceAll("(\\u00a0|&nbsp;|&#160;) ", " ");
        uncleanedRisHTML = uncleanedRisHTML.replaceAll(" (\\u00a0|&nbsp;|&#160;)", " ");


        Jerry fullHtml = Jerry.of(uncleanedRisHTML);

        //remove all sr-only content (screenreader only)
        fullHtml.find(".sr-only").remove();
        fullHtml.find("head").remove();

        //paperw can be multiple (annotated with "nextpage") - need to get content from all
        //by removing irrelevant ones
        Iterator<Jerry> pwIterator = fullHtml.find(".paperw>.contentBlock").iterator();

        boolean beforeTextElem = true;

        while (pwIterator.hasNext()) {
            Jerry element = pwIterator.next();

            //find title (first h1)
            Jerry titleElement = element.find("h1.Titel").first();

            if (titleElement != null) {
                String title = titleElement.text().trim();

                if (title.equalsIgnoreCase("Text") ||
                        title.equalsIgnoreCase("Kopf") ||
                        title.equalsIgnoreCase("Leitsatz") ||
                        title.equalsIgnoreCase("Rechtssatz") ||
                        title.equalsIgnoreCase("Spruch")) {
                    beforeTextElem = false;
                }

                //Usually at the end
                if (beforeTextElem || title.equalsIgnoreCase("European Case Law Identifier")) {
                    element.remove();
                }
            }
        }

        //word count (approx.)
        String fullText = fullHtml.text();
        fullText = fullText.replaceAll("[ ]+", " ");
        long wordCount = fullText.split(" ").length;

        //now, in html only the text should be left
        return fullHtml.htmlAll(true);
    }
}
