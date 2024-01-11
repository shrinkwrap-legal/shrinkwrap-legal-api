package legal.shrinkwrap.api.adapter.ris.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OgdDocumentHits (
	
	@JsonProperty("@pageNumber")
	String pageNumber,
	
	@JsonProperty("@pageSize")
	String pageSize,

	@JsonProperty("#text")
	String text
) {

}


