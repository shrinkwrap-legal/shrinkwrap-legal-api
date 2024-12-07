package legal.shrinkwrap.api.adapter.ris.rest.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

public class OgdDocumentReference {
	
	@JsonProperty("Data")
	private Data data;
	
	public Data getData() {
		return data;
	}
	
	public void setData(Data data) {
		this.data = data;
	}
	
	public static class Data {
		@JsonProperty("Metadaten")
		private Metadaten metadaten;
		
		@JsonProperty("Dokumentliste")
		private Dokumentliste dokumentliste;
		
		public Metadaten getMetadaten() {
			return metadaten;
		}
		
		public void setMetadaten(Metadaten metadaten) {
			this.metadaten = metadaten;
		}
		
		public Dokumentliste getDokumentliste() {
			return dokumentliste;
		}
		
		public void setDokumentliste(Dokumentliste dokumentliste) {
			this.dokumentliste = dokumentliste;
		}
		
	}
	
	public static class Metadaten {
		@JsonProperty("Technisch")
		private OgdMetadatenTechnisch technisch;

		@JsonProperty("Allgemein")
		private OgdMetadatenAllgemein allgemein;


		@JsonProperty("Bundesrecht")
		private Bundesrecht bundesrecht;
		
		
		public OgdMetadatenTechnisch getTechnisch() {
			return technisch;
		}
		public void setTechnisch(OgdMetadatenTechnisch technisch) {
			this.technisch = technisch;
		}
		
		public OgdMetadatenAllgemein getAllgemein() {
			return allgemein;
		}
		
		public void setAllgemein(OgdMetadatenAllgemein allgemein) {
			this.allgemein = allgemein;
		}
		
		public Bundesrecht getBundesrecht() {
			return bundesrecht;
		}
		public void setBundesrecht(Bundesrecht bundesrecht) {
			this.bundesrecht = bundesrecht;
		}

		

		
		public static class Bundesrecht {
			
			@JsonProperty("Kurztitel")
			private String kurztitel;
			
			@JsonProperty("Titel")
			private String titel;
			
			@JsonProperty("Eli")
			private String eli;
			
			@JsonProperty("BrKons")
			private BrKons brKons;
			
			public String getKurztitel() {
				return kurztitel;
			}
			
			public void setKurztitel(String kurztitel) {
				this.kurztitel = kurztitel;
			}
			
			public String getTitel() {
				return titel;
			}
			
			public void setTitel(String titel) {
				this.titel = titel;
			}
			
			public String getEli() {
				return eli;
			}
			
			public void setEli(String eli) {
				this.eli = eli;
			}
			
			public BrKons getBrKons() {
				return brKons;
			}
			
			public void setBrKons(BrKons brKons) {
				this.brKons = brKons;
			}
			
			public static class BrKons {
				@JsonProperty("Abkuerzung")
				private String abkuerzung;
				
				@JsonProperty("Inkrafttretensdatum")
				@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
				private LocalDate inkrafttretenDate;
				
				public String getAbkuerzung() {
					return abkuerzung;
				}
				
				public void setAbkuerzung(String abkuerzung) {
					this.abkuerzung = abkuerzung;
				}
				
				public LocalDate getInkrafttretenDate() {
					return inkrafttretenDate;
				}
				
				public void setInkrafttretenDate(LocalDate inkrafttretenDate) {
					this.inkrafttretenDate = inkrafttretenDate;
				}
				
				
			}
			
		}
	}
	
	public static class Dokumentliste {
		
		@JsonProperty("ContentReference")
		private ContentReference contentReference;
		
		public ContentReference getContentReference() {
			return contentReference;
		}
		
		public void setContentReference(ContentReference contentReference) {
			this.contentReference = contentReference;
		}
		
		
		public static class ContentReference {
			
			@JsonProperty("ContentType")
			private String contentType;
			
			@JsonProperty("Name")
			private String name;
			
			@JsonProperty("Urls")
			private DokumentlisteUrl urls;
		
			public String getContentType() {
				return contentType;
			}
			public void setContentType(String contentType) {
				this.contentType = contentType;
			}
			
			public String getName() {
				return name;
			}
			
			public void setName(String name) {
				this.name = name;
			}
			
			public DokumentlisteUrl getUrls() {
				return urls;
			}
			
			public void setUrls(DokumentlisteUrl urls) {
				this.urls = urls;
			}
			
			
			public static class DokumentlisteUrl {
			
				@JsonProperty("ContentUrl")
				private List<ContentUrl> contentUrls;
				
				public List<ContentUrl> getContentUrls() {
					return contentUrls;
				}
				
				public void setContentUrls(List<ContentUrl> contentUrls) {
					this.contentUrls = contentUrls;
				}
				
				public static class ContentUrl {
					@JsonProperty("DataType")
					private String dataType;
					
					@JsonProperty("Url")
					private String url;
					
					public String getDataType() {
						return dataType;
					}
					public void setDataType(String dataType) {
						this.dataType = dataType;
					}
					public String getUrl() {
						return url;
					}
					public void setUrl(String url) {
						this.url = url;
					}
				}
				
			}
		}
		
	}
	

}
