package legal.shrinkwrap.api.adapter.ris.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class OgdDocumentResults {
	
	@JsonProperty("Hits")
	private OgdDocumentHits hits;
	
	@JsonProperty("OgdDocumentReference")
	private List<OgdDocumentReference> documentList;
	
	
	public OgdDocumentHits getHits() {
		return hits;
	}
	
	public List<OgdDocumentReference> getDocumentList() {
		return documentList;
	}
	


}
