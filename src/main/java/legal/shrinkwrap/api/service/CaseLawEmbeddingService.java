package legal.shrinkwrap.api.service;

import legal.shrinkwrap.api.dto.CaselawSummaryCivilCase;
import legal.shrinkwrap.api.persistence.entity.CaseLawEmbeddingEntity;
import legal.shrinkwrap.api.persistence.entity.CaseLawEntity;
import legal.shrinkwrap.api.persistence.repo.CaseLawEmbeddingRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class CaseLawEmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final CaseLawEmbeddingRepository caseLawEmbeddingRepository;
    private final TokenCountEstimator tokenCountEstimator = new JTokkitTokenCountEstimator();

    public CaseLawEmbeddingEntity getAndStoreVectorForCaseLawSummary(CaseLawEntity entity, CaselawSummaryCivilCase caselawSummaryCivilCase) {
        CaseLawEmbeddingEntity embedding = getVectorForCaseLawSummary(caselawSummaryCivilCase);
        embedding.setCaseLaw(entity);
        embedding = caseLawEmbeddingRepository.save(embedding);
        return embedding;
    }

    public CaseLawEmbeddingEntity getVectorForCaseLawSummary(CaselawSummaryCivilCase caselawSummaryCivilCase) {
        //transform the json to some YAML, in order to save token
        StringBuilder summary = new StringBuilder();
        summary.append("Titelvariante: " + caselawSummaryCivilCase.getZeitungstitel_rechtszeitschrift());
        summary.append("\nTitelvariante: " + caselawSummaryCivilCase.getZeitungstitel_boulevard());
        summary.append("\nTitelvariante: " + caselawSummaryCivilCase.getZeitungstitel_oeffentlich());
        summary.append("\nSachverhalt: " + caselawSummaryCivilCase.getSachverhalt());
        summary.append("\nBegehren: " + caselawSummaryCivilCase.getBegehren());
        summary.append("\nGegner: " + caselawSummaryCivilCase.getGegenvorbringen());
        summary.append("\nGericht: " + caselawSummaryCivilCase.getEntscheidung_gericht());
        summary.append("\nSchlussfolgerungen: " + caselawSummaryCivilCase.getSchlussfolgerungen().stream().collect(StringBuilder::new, StringBuilder::append, StringBuilder::append));
        summary.append("\nZusammenfassung: " + caselawSummaryCivilCase.getZusammenfassung_3_saetze() + "");
        String fullSummary = summary.toString();

        //check if absaetze still fits
        String absaetze = "\nZusammenfassung: " + caselawSummaryCivilCase.getZusammenfassung_3_absaetze().stream().collect(StringBuilder::new, StringBuilder::append, StringBuilder::append);
        int estimate = tokenCountEstimator.estimate(fullSummary + absaetze);
        if (estimate < 8192) {
            fullSummary += absaetze;
        }

        EmbeddingResponse embeddingResponse = this.embeddingModel.embedForResponse(List.of(fullSummary));

        CaseLawEmbeddingEntity ret = new CaseLawEmbeddingEntity();
        ret.setEmbedding(embeddingResponse.getResult().getOutput());
        ret.setContent(fullSummary);
        return ret;
    }
}
