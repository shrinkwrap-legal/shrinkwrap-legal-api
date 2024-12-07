package legal.shrinkwrap.api.adapter.ris.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OgdSearchResult (
	
	@JsonProperty("OgdDocumentResults")
	OgdDocumentResults ogdDocumentResults

){
}

