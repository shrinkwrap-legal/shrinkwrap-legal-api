package legal.shrinkwrap.api.service;

import legal.shrinkwrap.api.persistence.entity.CaseLawAnalysisEntity;
import legal.shrinkwrap.api.persistence.entity.CaseLawEntity;
import legal.shrinkwrap.api.persistence.entity.CommonSentences;
import legal.shrinkwrap.api.persistence.repo.CaseLawAnalysisRepository;
import legal.shrinkwrap.api.persistence.repo.CaseLawRepository;
import legal.shrinkwrap.api.persistence.repo.CommonSentencesRepository;
import legal.shrinkwrap.api.utils.SentenceHashingTools;
import lombok.extern.slf4j.Slf4j;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CommonSentenceService {

    private Trie trie;

    private final CommonSentencesRepository commonSentencesRepository;

    private final CaseLawRepository caseLawRepository;
    private final CaseLawAnalysisRepository caseLawAnalysisRepository;

    public CommonSentenceService(CommonSentencesRepository commonSentencesRepository, CaseLawRepository caseLawRepository, CaseLawAnalysisRepository caseLawAnalysisRepository) {
        this.commonSentencesRepository = commonSentencesRepository;
        this.caseLawRepository = caseLawRepository;
        this.caseLawAnalysisRepository = caseLawAnalysisRepository;
    }

    public void buildTree() {
        Trie.TrieBuilder trieBuilder = Trie.builder();

        List<String> allSentences = commonSentencesRepository.findAllSentenceHash();
        for (String sentences : allSentences) {
            trieBuilder.addKeyword(sentences);
        }

        log.info("building tree with " + allSentences.size() + " sentences");
        this.trie = trieBuilder.build();
        log.info("trie built with " + allSentences.size() + " sentences");
    }

    public List<String> findContainedSentences(String sentenceHash) {
        if (this.trie == null) {
            buildTree();
        }

        Collection<Emit> emits = this.trie.parseText(sentenceHash);
        List<String> matches = emits.stream().map(emit -> emit.getKeyword()).collect(Collectors.toCollection(ArrayList::new));
        return matches;
    }

    public void importFromECLITextFile(String fullString) {
        AtomicInteger num = new AtomicInteger(0);
        for (String sentence : fullString.split("\n")) {
            String ecli = sentence.split(";")[0].trim();
            String fragment = sentence.split(";",2)[1].trim();

            //check first if sentence not already in DB
            Optional<CommonSentences> firstBySentenceHash = commonSentencesRepository.findFirstBySentenceHash(fragment);
            if (firstBySentenceHash.isPresent()) {
                continue;
            }

            Optional<CaseLawEntity> caseLawEntityByEcli = caseLawRepository.findCaseLawEntityByEcli(ecli);
            if (!caseLawEntityByEcli.isPresent()) {
                log.warn("no caselaw found for " + ecli);
                continue;
            }
            Optional<CaseLawAnalysisEntity> analysisEntity = caseLawAnalysisRepository.findFirstByAnalysisTypeAndCaseLaw_IdOrderByAnalysisVersionDesc("text", caseLawEntityByEcli.get().getId());
            if (!analysisEntity.isPresent()) {
                log.warn("no analysis found for " + ecli);
                continue;
            }

            CommonSentences sentencesEntity = new CommonSentences();
            sentencesEntity.setCaseLaw(caseLawEntityByEcli.get());
            sentencesEntity.setSentenceHash(fragment);
            sentencesEntity.setApplicationType(caseLawEntityByEcli.get().getApplicationType());

            String fullTextOfFragment = SentenceHashingTools.getCommonSentence(analysisEntity.get().getFullText(), fragment);
            sentencesEntity.setFullText(fullTextOfFragment);

            if (fullTextOfFragment == null) {
                log.warn("no match found for " + fragment  + " for " + ecli);
                continue;
            }

            commonSentencesRepository.save(sentencesEntity);
            if (num.incrementAndGet() % 10 == 0) {
                log.info("imported " + num.get() + " sentences");
            }
        }
    }
}
