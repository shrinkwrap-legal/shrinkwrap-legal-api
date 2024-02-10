package legal.shrinkwrap.api.adapter.ris;

import legal.shrinkwrap.api.adapter.ris.dto.OgdSearchResult;
import legal.shrinkwrap.api.adapter.ris.dto.enums.OgdApplikationEnum;

/**
 * <a href="https://data.bka.gv.at/ris/ogd/v2.6/Documents/Dokumentation_OGD-RIS_API.pdf">RIS API Documentation</a>
 *
 */
public interface RisAdapter {

    /**
     *
     * @return version of ris rest api
     */
    String getVersion();

    String getCaselawByDocNumberAsHtml(OgdApplikationEnum app, String docNumber);

    OgdSearchResult getJustiz(OgdApplikationEnum app, String rechtssatznummer);

}
