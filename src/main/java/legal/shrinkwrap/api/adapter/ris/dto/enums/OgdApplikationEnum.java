package legal.shrinkwrap.api.adapter.ris.dto.enums;

public enum OgdApplikationEnum {
	
	BundesrechtKonsolidiert("BrKons"),
	LandesrechtKonsolidiert("LrKons"),

	Justiz("Justiz")

	;
	
	
	private final String technisch;
	
	private OgdApplikationEnum(String technisch) {
		this.technisch = technisch;
	}
	
	public String getTechnisch() {
		return technisch;
	}
	

}
