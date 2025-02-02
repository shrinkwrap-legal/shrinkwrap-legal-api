package legal.shrinkwrap.api.adapter.ris;


import legal.shrinkwrap.api.adapter.ris.dto.RisCourt;
import legal.shrinkwrap.api.adapter.ris.dto.RisSearchResult;


public record RisSearchParameterCaseLaw(
        RisCourt court,
        String ecli,
        String docNumber,
        JudikaturTyp judikaturTyp
) {
    //Builder
    public static final class RisSearchParameterCaseLawBuilder {


        private RisCourt court;
        private String ecli;
        private String docNumber;
        private RisSearchParameterCaseLaw.JudikaturTyp judikaturTyp = new JudikaturTyp(true, true);

        public RisSearchParameterCaseLawBuilder court(RisCourt court) {
            this.court = court;
            return this;
        }

        public RisSearchParameterCaseLawBuilder ecli(String ecli) {
            this.ecli = ecli;
            return this;
        }

        public RisSearchParameterCaseLawBuilder docNumber(String docNumber) {
            this.docNumber = docNumber;
            return this;
        }

        public RisSearchParameterCaseLawBuilder judikaturTyp(RisSearchParameterCaseLaw.JudikaturTyp judikaturTyp) {
            this.judikaturTyp = judikaturTyp;
            return this;
        }

        public RisSearchParameterCaseLaw build() {
            return new RisSearchParameterCaseLaw(court, ecli, docNumber, judikaturTyp);
        }

    }

    public static RisSearchParameterCaseLaw.RisSearchParameterCaseLawBuilder builder() {
        return new RisSearchParameterCaseLawBuilder();
    }

    public record JudikaturTyp(Boolean inRechtssaetzen, Boolean inEntscheidungstexten) {
    }
}
