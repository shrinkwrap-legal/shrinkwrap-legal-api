package legal.shrinkwrap.api.adapter.ris;

import legal.shrinkwrap.api.adapter.ris.dto.RisCourt;
import legal.shrinkwrap.api.adapter.ris.dto.RisSearchResult;

public interface RisSoapAdapter {

    String getVersion();

    RisSearchResult findCaseLawDocuments(RisCourt court);


}
