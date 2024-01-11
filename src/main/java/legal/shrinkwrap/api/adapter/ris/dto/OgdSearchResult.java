package legal.shrinkwrap.api.adapter.ris.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OgdSearchResult (
	
	@JsonProperty("OgdDocumentResults")
	OgdDocumentResults ogdDocumentResults

){
}

