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
    String ausgang;
    String rechtsmittel;
    String sachverhalt;
    @JsonAlias("begehren_angeklagter")
    String begehren;
    @JsonAlias("begehren_staatsanwaltschaft")
    String gegenvorbringen;
    String berufende_partei;
    List<String> zusammenfassung_3_absaetze;
    String zusammenfassung_3_saetze;
    String zeitungstitel_boulevard;
    String zeitungstitel_oeffentlich;
    String zeitungstitel_rechtszeitschrift;
    List<String> schlussfolgerungen;
    List<String> wichtige_normen;
    List<String> hauptrechtsgebiete;
    List<String> unterrechtsgebiete;
}
