package legal.shrinkwrap.api.adapter.ris.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record OgdDocumentResults (
	
	@JsonProperty("Hits")
	OgdDocumentHits hits,

	@JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
	@JsonProperty("OgdDocumentReference")
	List<OgdDocumentReference> documentList){

}
