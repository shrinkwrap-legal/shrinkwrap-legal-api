package legal.shrinkwrap.api.adapter.ris.dto.enums;

public enum ApplikationEnum {
	
	BundesrechtKonsolidiert("BrKons"),
	LandesrechtKonsolidiert("LrKons")
	;
	
	
	private final String technisch;
	
	private ApplikationEnum(String technisch) {
		this.technisch = technisch;
	}
	
	public String getTechnisch() {
		return technisch;
	}
	

}
