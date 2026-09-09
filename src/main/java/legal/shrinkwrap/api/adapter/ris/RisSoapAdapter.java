package legal.shrinkwrap.api.adapter.ris;

import legal.shrinkwrap.api.adapter.ris.dto.RisNormResult;
import legal.shrinkwrap.api.adapter.ris.dto.RisSearchResult;

import java.util.List;

public interface RisSoapAdapter {

    String getVersion();

    RisSearchResult findCaseLawDocuments(RisSearchParameterCaseLaw searchParameter);

    /** Consolidated law, federal or of one state; every version unless a Fassung is given. */
    List<RisNormResult> findNormDocuments(RisSearchParameterNorm searchParameter);

    /**
     * Norm documents RIS has changed recently, from its change feed. Includes withdrawn ones -
     * a provision that disappears has to reach the import so it can be marked as gone.
     */
    List<RisNormResult> findChangedNormDocuments(boolean landesrecht, int changedInLastXDays);


}
