package legal.shrinkwrap.api.adapter;

import org.springframework.web.client.RestClient;

public class HtmlDownloadService {

    private RestClient restClient = RestClient.create();

    public String downloadHtml(String url) {
        return restClient.get().uri(url).retrieve().body(String.class);
    }
}
