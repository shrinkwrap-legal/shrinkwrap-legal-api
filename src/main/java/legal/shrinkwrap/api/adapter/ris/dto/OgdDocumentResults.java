package legal.shrinkwrap.api.adapter.ris.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class OgdDocumentResults {
	
	@JsonProperty("Hits")
	private OgdDocumentHits hits;
	
	@JsonProperty("OgdDocumentReference")
	private List<me.jungwirth.playground.springboot.legaltech.adapter.ris.dto.OgdDocumentReference> documentList;
	
	
	public OgdDocumentHits getHits() {
		return hits;
	}
	
	public List<me.jungwirth.playground.springboot.legaltech.adapter.ris.dto.OgdDocumentReference> getDocumentList() {
		return documentList;
	}
	


}
