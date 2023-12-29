package legal.shrinkwrap.api.adapter.ris.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OgdSearchResponse {
	
	@JsonProperty(value = "OgdSearchResult")
	private OgdSearchResult ogdSearchResult;
	
	public OgdSearchResult getOgdSearchResult() {
		return ogdSearchResult;
	}
	
	public void setOgdSearchResult(OgdSearchResult ogdSearchResult) {
		this.ogdSearchResult = ogdSearchResult;
	}

}
