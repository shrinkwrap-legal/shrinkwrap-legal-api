package legal.shrinkwrap.api.utils;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class SentenceHashingTools {

    private static String prepareSentenceForHashing(String sentence) {
        //adjust blank spaces, trim
        return sentence.replaceAll("\\u00a0|&nbsp;|&#160;", " ")
                .replaceAll("\n", " ")
                .replaceAll("\r", " ")
                .replaceAll("\\s", " ")
                .replaceAll("(\\s)+", " ").trim();
    }

    public static List<HashedSentence> getSentenceModel(String fullText) {
        int minSentenceLength = 10;

        String[] sentences = fullText.split("\\.");
        List<Character> bytes = new ArrayList<>();
        List<HashedSentence> sentenceToChar = new ArrayList<>();

        //for each sentence of minimal length, assign a character
        for (int i=0;i<sentences.length;i++) {
            String sentence = prepareSentenceForHashing(sentences[i]);

            if (sentence.length() < minSentenceLength) {
                continue;
            }

            String nextSentence = "";
            int j;
            //each "pair" should consist of at least two sentences fulfilling the criteria
            for (j=i+1;j<sentences.length;j++) {
                nextSentence = prepareSentenceForHashing(sentences[j]);
                if (nextSentence.length() > minSentenceLength) {
                    break;
                }
            }

            String sentenceForHash = sentence + " | " + nextSentence;

            byte asByte = DigestUtils.md5(sentenceForHash)[0];
            char oneChar = (char) (((asByte & 0xFF) % 95) + 32);
            bytes.add(oneChar);

            HashedSentence hs = new HashedSentence();
            hs.setBeginPos(i);
            hs.setEndPos(j);
            hs.setCharacter(oneChar);
            hs.setSentence(sentence);
            hs.setSentenceWithNext(sentenceForHash);

            sentenceToChar.add(hs);
        }

        return sentenceToChar;
    }

    public static String replaceCommonSentence(String fullText, List<List<HashedSentence>> sentencesToReplace) {
        //get original sentences again
        String[] sentences = fullText.split("\\.");

        //order sentencesToReplace by beginPos
        sentencesToReplace.sort(Comparator.comparingInt(o -> o.get(0).getBeginPos()));

        StringBuilder resultingText = new StringBuilder();

        //in a stringbuilder, rearrange all sentences but not the ones within each begin and endpos

        int lastEndPos = 0;
        for (List<HashedSentence> sentenceList : sentencesToReplace) {
            int beginPos = sentenceList.get(0).getBeginPos();
            String sentenceBefore = String.join(".", Arrays.copyOfRange(sentences, lastEndPos, beginPos));
            resultingText.append(sentenceBefore).append(".").append(" (...) ");

            lastEndPos = sentenceList.get(sentenceList.size()-1).getEndPos();
        }

        //add last segment
        String lastSentence = String.join(".", Arrays.copyOfRange(sentences, lastEndPos, sentences.length));
        resultingText.append(".").append(lastSentence);

        return resultingText.toString();
    }

    @Getter
    @Setter
    public static class HashedSentence {
        public Character character;
        public String sentence;
        public String sentenceWithNext;
        private int beginPos;
        private int endPos;
    }
}
