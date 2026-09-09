package legal.shrinkwrap.api.adapter.ris;

import at.gv.bka.ris.v26.soap.ws.client.Bundesland;
import at.gv.bka.ris.v26.soap.ws.client.NormabschnittTyp;

import java.time.LocalDate;

/**
 * Search parameters for consolidated law, federal (BrKons) and state (LrKons).
 *
 * @param bundesland    null searches federal law, a value searches that state
 * @param gesetzesnummer the RIS law number - the only criterion that reliably brings up a
 *                       complete law, see the norm concept document
 * @param abschnittTyp  only takes effect together with {@link #abschnittVon}/{@link #abschnittBis};
 *                      RIS ignores it silently on its own, which turns a targeted request into a
 *                      full scan of the corpus. {@link #build()} therefore rejects the half-set case
 * @param fassungVom    as-of date; without it RIS answers with every version of the provision
 */
public record RisSearchParameterNorm(
        Bundesland bundesland,
        String gesetzesnummer,
        String titel,
        NormabschnittTyp abschnittTyp,
        String abschnittVon,
        String abschnittBis,
        LocalDate fassungVom
) {
    public boolean isLandesrecht() {
        return bundesland != null;
    }

    //Builder
    public static final class RisSearchParameterNormBuilder {

        private Bundesland bundesland;
        private String gesetzesnummer;
        private String titel;
        private NormabschnittTyp abschnittTyp;
        private String abschnittVon;
        private String abschnittBis;
        private LocalDate fassungVom;

        public RisSearchParameterNormBuilder bundesland(Bundesland bundesland) {
            this.bundesland = bundesland;
            return this;
        }

        public RisSearchParameterNormBuilder gesetzesnummer(String gesetzesnummer) {
            this.gesetzesnummer = gesetzesnummer;
            return this;
        }

        public RisSearchParameterNormBuilder titel(String titel) {
            this.titel = titel;
            return this;
        }

        /** Von and bis are strings because a section is "12a" as readily as "12". */
        public RisSearchParameterNormBuilder abschnitt(NormabschnittTyp typ, String von, String bis) {
            this.abschnittTyp = typ;
            this.abschnittVon = von;
            this.abschnittBis = bis;
            return this;
        }

        public RisSearchParameterNormBuilder fassungVom(LocalDate fassungVom) {
            this.fassungVom = fassungVom;
            return this;
        }

        public RisSearchParameterNorm build() {
            int parts = (abschnittTyp != null ? 1 : 0)
                    + (abschnittVon != null ? 1 : 0)
                    + (abschnittBis != null ? 1 : 0);
            if (parts != 0 && parts != 3) {
                throw new IllegalArgumentException(
                        "Abschnitt needs typ, von and bis together - RIS drops an incomplete "
                                + "restriction without an error and returns the whole corpus");
            }
            return new RisSearchParameterNorm(bundesland, gesetzesnummer, titel,
                    abschnittTyp, abschnittVon, abschnittBis, fassungVom);
        }
    }

    public static RisSearchParameterNormBuilder builder() {
        return new RisSearchParameterNormBuilder();
    }
}
