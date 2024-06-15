package legal.shrinkwrap.api.service;

import jodd.jerry.Jerry;
import legal.shrinkwrap.api.controller.CaseLawController;
import legal.shrinkwrap.api.dto.CaseLawResponseDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Service
public class CaselawTextService {

    /**
     * Clean up RIS html response
     * @param uncleanedRisHTML
     * @return
     */
    public CaseLawResponseDto prepareRISCaseLawHtml(String uncleanedRisHTML) {
        Jerry fullHtml = Jerry.of(uncleanedRisHTML);

        //remove all sr-only content (screenreader only)
        fullHtml.find(".sr-only").remove();
        fullHtml.find("head").remove();

        //paperw can be multiple (annotated with "nextpage")
        Iterator<Jerry> pwIterator = fullHtml.find(".paperw>.contentBlock").iterator();
        Map<String, String> metaInfo = new HashMap<>();

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

                if (beforeTextElem || title.equalsIgnoreCase("European Case Law Identifier")) {
                    String content = element.find("*").not("h1.Titel").text();
                    if (StringUtils.isNotEmpty(content)) {
                        metaInfo.put(title, content);
                    }

                    element.remove();
                }
            }
        }

        //word count (approx.)
        String fullText = fullHtml.text();
        fullText = fullText.replaceAll("[ ]+", " ");
        int wordCount = fullText.split(" ").length;

        CaseLawResponseDto response = new CaseLawResponseDto(wordCount, fullHtml.htmlAll(true), metaInfo);

        //now, in html only the text should be left
        return response;
    }
}
