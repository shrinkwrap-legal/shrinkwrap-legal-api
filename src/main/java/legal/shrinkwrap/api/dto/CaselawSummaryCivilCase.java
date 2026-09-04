package legal.shrinkwrap.api.dto;


import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class CaselawSummaryCivilCase {
    @JsonProperty(required = true)
    String art;
    @JsonAlias("eugh_vorlage")
    Boolean eugh;
    String ausgang;
    String rechtsmittel;
    String verfahrensart;
    @JsonPropertyDescription("The case summary or subject matter - what the deciding court established to have happened that lead to this case")
    String sachverhalt;
    @JsonAlias({"begehren_angeklagter", "begehren_beschwerdefuehrer"})
    @JsonPropertyDescription("The plaintiff's or complainant's claim - what he/she/the company/authority is seeking in the lawsuit")
    String begehren;
    @JsonAlias({"begehren_staatsanwaltschaft", "gegenargumente_staat", "gegenargumente_staat_oder_behoerde", "gegenargumente_behoerde", "gegenargumente_gegenpartei"})
    @JsonPropertyDescription("The opposing argument or counterclaim - what the opposing party is seeking in the lawsuit. In many cases, the opposing party will be an authority, state or the government")
    String gegenvorbringen;
    String berufende_partei;
    @JsonAlias({"entscheidung_gericht_1_saetze", "entscheidung_gericht_2_saetze", "entscheidung_gericht_3_saetze", "entscheidung_gericht_4_saetze","entscheidung_gericht_5_saetze"})
    @JsonPropertyDescription("The final decision as established by the court - what the deciding court established as the outcome of the case")
    String entscheidung_gericht;
    @JsonAlias({"zusammenfassung_1_absaetze", "zusammenfassung_2_absaetze", "zusammenfassung_4_absaetze", "zusammenfassung_5_absaetze"})
    @JsonPropertyDescription("A summary of the court's decision - a brief overview of the court's decision in the case")
    List<String> zusammenfassung_3_absaetze;
    @JsonAlias({"zusammenfassung_1_saetze", "zusammenfassung_2_saetze", "zusammenfassung_4_saetze", "zusammenfassung_5_saetze"})
    String zusammenfassung_3_saetze;
    String zeitungstitel_boulevard;
    String zeitungstitel_oeffentlich;
    String zeitungstitel_rechtszeitschrift;
    @JsonAlias("schlussfolgerungen_gericht")
    @JsonPropertyDescription("A list of the legal reasoning of the court's decision that lead to the final decision")
    List<String> schlussfolgerungen;
    @JsonPropertyDescription("A list of legal norms that were applied in this case. In the list, they will begin with a abbreviation of the norm")
    List<String> wichtige_normen;
    List<String> hauptrechtsgebiete;
    List<String> unterrechtsgebiete;
}
