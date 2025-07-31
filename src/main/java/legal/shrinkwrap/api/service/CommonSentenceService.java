package legal.shrinkwrap.api.service;

import legal.shrinkwrap.api.persistence.repo.CommonSentencesRepository;
import lombok.extern.slf4j.Slf4j;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
@Slf4j
public class CommonSentenceService {

    private Trie trie;

    private final CommonSentencesRepository commonSentencesRepository;

    public CommonSentenceService(CommonSentencesRepository commonSentencesRepository) {
        this.commonSentencesRepository = commonSentencesRepository;
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
        List<String> matches = emits.stream().map(emit -> emit.getKeyword()).toList();
        return matches;
    }
}
