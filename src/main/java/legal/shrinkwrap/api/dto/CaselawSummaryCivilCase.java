package legal.shrinkwrap.api.dto;


import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
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
    @JsonAlias({"begehren_angeklagter", "begehren_beschwerdefuehrer"})
    String begehren;
    @JsonAlias({"begehren_staatsanwaltschaft", "gegenargumente_staat", "gegenargumente_staat_oder_behoerde", "gegenargumente_behoerde", "gegenargumente_gegenpartei"})
    String gegenvorbringen;
    String berufende_partei;
    @JsonAlias({"entscheidung_gericht_1_saetze", "entscheidung_gericht_2_saetze", "entscheidung_gericht_3_saetze", "entscheidung_gericht_4_saetze","entscheidung_gericht_5_saetze"})
    String entscheidung_gericht;
    @JsonAlias({"zusammenfassung_1_absaetze", "zusammenfassung_2_absaetze", "zusammenfassung_4_absaetze", "zusammenfassung_5_absaetze"})
    List<String> zusammenfassung_3_absaetze;
    @JsonAlias({"zusammenfassung_1_saetze", "zusammenfassung_2_saetze", "zusammenfassung_4_saetze", "zusammenfassung_5_saetze"})
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
