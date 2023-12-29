package legal.shrinkwrap.api.adapter.ris.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OgdSearchResult {
	
	@JsonProperty("OgdDocumentResults")
	private OgdDocumentResults ogdDocumentResults;
	
	
	public OgdDocumentResults getOgdDocumentResults() {
		return ogdDocumentResults;
	}
	
	public void setOgdDocumentResults(OgdDocumentResults ogdDocumentResults) {
		this.ogdDocumentResults = ogdDocumentResults;
	}
	

}
