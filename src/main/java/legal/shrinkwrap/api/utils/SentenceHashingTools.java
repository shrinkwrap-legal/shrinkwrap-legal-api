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

        String [] sentences = splitFullText(fullText);
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

    private static String[] splitFullText(String fullText) {
        //split at "." and newline - for this, insert split characters
        fullText = fullText.replaceAll("\\.",".<SPLITHERE>");
        fullText = fullText.replaceAll("\n","\n<SPLITHERE>");

        String[] sentences = fullText.split("<SPLITHERE>");
        return sentences;
    }

    public static String getCommonSentence(String fullText, String sentence) {
        List<HashedSentence> sentenceModel = getSentenceModel(fullText);
        return getCommonSentence(fullText, sentence, sentenceModel);
    }

    public static String getCommonSentence(String fullText, String sentence, List<HashedSentence> sentenceModel) {
        String[] sentences = splitFullText(fullText);

        //find sequence that is given
        String fullSentence = getHashFromModel(sentenceModel);

        int startPos = fullSentence.indexOf(sentence);
        if (startPos == -1) {
            return null;
        }

        int beginPos = sentenceModel.get(startPos).getBeginPos();
        int endPos = sentenceModel.get(startPos+sentence.length()-1).getEndPos();
        return String.join("", Arrays.copyOfRange(sentences, beginPos, endPos+1));
    }

    public static String replaceCommonSentence(String fullText, List<List<HashedSentence>> sentencesToReplace) {
        //get original sentences again
        String[] sentences = splitFullText(fullText);

        //order sentencesToReplace by beginPos
        sentencesToReplace.sort(Comparator.comparingInt(o -> o.get(0).getBeginPos()));

        StringBuilder resultingText = new StringBuilder();

        //in a stringbuilder, rearrange all sentences but not the ones within each begin and endpos

        int lastEndPos = 0;
        for (List<HashedSentence> sentenceList : sentencesToReplace) {
            int beginPos = sentenceList.get(0).getBeginPos();
            String sentenceBefore = String.join("", Arrays.copyOfRange(sentences, lastEndPos, beginPos));
            resultingText.append(sentenceBefore).append(" (...) ");

            lastEndPos = sentenceList.get(sentenceList.size()-1).getEndPos();
        }

        //add last segment
        String lastSentence = String.join("", Arrays.copyOfRange(sentences, lastEndPos, sentences.length));
        resultingText.append(lastSentence);

        return resultingText.toString();
    }

    public static String getHashFromModel(List<HashedSentence> sentenceModel) {
        return sentenceModel.stream().map(HashedSentence::getCharacter).collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();
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
