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
    @JsonProperty(required = false)
    Boolean eugh;
    @JsonProperty(required = false)
    String ausgang;
    @JsonProperty(required = false)
    String rechtsmittel;
    @JsonProperty(required = false)
    String verfahrensart;
    @JsonPropertyDescription("The case summary or subject matter - what the deciding court established to have happened that lead to this case")
    @JsonProperty(required = false)
    String sachverhalt;
    @JsonAlias({"begehren_angeklagter", "begehren_beschwerdefuehrer"})
    @JsonPropertyDescription("The plaintiff's or complainant's claim - what he/she/the company/authority is seeking in the lawsuit")
    @JsonProperty(required = false)
    String begehren;
    @JsonAlias({"begehren_staatsanwaltschaft", "gegenargumente_staat", "gegenargumente_staat_oder_behoerde", "gegenargumente_behoerde", "gegenargumente_gegenpartei"})
    @JsonPropertyDescription("The opposing argument or counterclaim - what the opposing party is seeking in the lawsuit. In many cases, the opposing party will be an authority, state or the government")
    @JsonProperty(required = false)
    String gegenvorbringen;
    @JsonProperty(required = false)
    String berufende_partei;
    @JsonAlias({"entscheidung_gericht_1_saetze", "entscheidung_gericht_2_saetze", "entscheidung_gericht_3_saetze", "entscheidung_gericht_4_saetze","entscheidung_gericht_5_saetze"})
    @JsonPropertyDescription("The final decision as established by the court - what the deciding court established as the outcome of the case")
    @JsonProperty(required = false)
    String entscheidung_gericht;
    @JsonAlias({"zusammenfassung_1_absaetze", "zusammenfassung_2_absaetze", "zusammenfassung_4_absaetze", "zusammenfassung_5_absaetze"})
    @JsonPropertyDescription("A summary of the court's decision - a brief overview of the court's decision in the case")
    @JsonProperty(required = false)
    List<String> zusammenfassung_3_absaetze;
    @JsonAlias({"zusammenfassung_1_saetze", "zusammenfassung_2_saetze", "zusammenfassung_4_saetze", "zusammenfassung_5_saetze"})
    @JsonProperty(required = false)
    String zusammenfassung_3_saetze;
    @JsonProperty(required = false)
    String zeitungstitel_boulevard;
    @JsonProperty(required = false)
    String zeitungstitel_oeffentlich;
    @JsonProperty(required = false)
    String zeitungstitel_rechtszeitschrift;
    @JsonAlias("schlussfolgerungen_gericht")
    @JsonPropertyDescription("A list of the legal reasoning of the court's decision that lead to the final decision")
    @JsonProperty(required = false)
    List<String> schlussfolgerungen;
    @JsonPropertyDescription("A list of legal norms that were applied in this case. In the list, they will begin with a abbreviation of the norm")
    @JsonProperty(required = false)
    List<String> wichtige_normen;
    @JsonProperty(required = false)
    List<String> hauptrechtsgebiete;
    @JsonProperty(required = false)
    List<String> unterrechtsgebiete;
}
