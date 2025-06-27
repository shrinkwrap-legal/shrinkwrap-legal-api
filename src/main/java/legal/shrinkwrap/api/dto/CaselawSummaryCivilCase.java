package legal.shrinkwrap.api.dto;


import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CaselawSummaryCivilCase {
    @JsonProperty(required = true)
    String art;
    @JsonAlias("eugh_vorlage")
    Boolean eugh;
    String ausgang;
    String rechtsmittel;
    String verfahrensart;
    String sachverhalt;
    @JsonAlias({"begehren_angeklagter","begehren_beschwerdefuehrer"})
    String begehren;
    @JsonAlias({"begehren_staatsanwaltschaft","gegenargumente_staat"})
    String gegenvorbringen;
    String berufende_partei;
    @JsonAlias("entscheidung_gericht_2_saetze")
    String entscheidung_gericht;
    List<String> zusammenfassung_3_absaetze;
    String zusammenfassung_3_saetze;
    String zeitungstitel_boulevard;
    String zeitungstitel_oeffentlich;
    String zeitungstitel_rechtszeitschrift;
    @JsonAlias("schlussfolgerungen_gericht")
    List<String> schlussfolgerungen;
    List<String> wichtige_normen;
    List<String> hauptrechtsgebiete;
    List<String> unterrechtsgebiete;
}
