package legal.shrinkwrap.api.adapter.ris.dto;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;

public class RisSearchResult {

    private List<RisJudikaturResult> judikaturResults;

    public RisSearchResult() {}

    public RisSearchResult(List<RisJudikaturResult> judikaturResults) {
        this.judikaturResults = judikaturResults;
    }

    public void setJudikaturResults(List<RisJudikaturResult> judikaturResults) {
        this.judikaturResults = judikaturResults;
    }

    public List<RisJudikaturResult> getJudikaturResults() {
        return judikaturResults;
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("judikaturResults", judikaturResults)
                .toString();
    }
}
