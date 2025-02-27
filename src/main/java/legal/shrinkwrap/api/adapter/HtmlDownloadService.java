package legal.shrinkwrap.api.adapter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.web.client.RestClient;

public class HtmlDownloadService {

    private RestClient restClient = RestClient.create();

    public String downloadHtml(String url) {
        return restClient.get().uri(url).retrieve().body(String.class);
    }

    public String html2text(String html) {
        // return Jsoup.parse(html).wholeText();
        Document document = Jsoup.parse(html);
        document.outputSettings(document.outputSettings().prettyPrint(false));
        document.select("br").append("\\n");
        document.select("p").append("\\n\\n");
        String text = document.html().replaceAll("\\\\n", "\n");
        return Jsoup.clean(text, "", Safelist.none(), new Document.OutputSettings().prettyPrint(false));

    }
}

