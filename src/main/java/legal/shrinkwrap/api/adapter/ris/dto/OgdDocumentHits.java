package legal.shrinkwrap.api.adapter.ris.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OgdDocumentHits {
	
	@JsonProperty("@pageNumber")
	private String pageNumber;
	
	@JsonProperty("@pageSize")
	private String pageSize;
	
	
	public String getPageNumber() {
		return pageNumber;
	}

	public void setPageNumber(String pageNumber) {
		this.pageNumber = pageNumber;
	}
	
	public String getPageSize() {
		return pageSize;
	}
	
	public void setPageSize(String pageSize) {
		this.pageSize = pageSize;
	}
}


