package legal.shrinkwrap.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.mezzdev.suffixtree.GeneralizedSuffixTree;
import net.mezzdev.suffixtree.Pair;
import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
public class CommonSentenceFinderTest {
    ObjectMapper om = new ObjectMapper();

    static int MIN_JUDGEMENTS = 10;
    static int MIN_MONTHS = 3;
    static int MIN_LENGTH = 15;

    @Test
    @Disabled
    public void testFindCommonSubstringWithData() throws IOException, URISyntaxException {
        URL resource = getClass().getClassLoader().getResource("bvwg-2024.json");
        assertNotNull(resource, "Resource not found!");
        List<CourtCaseRecord> courtCaseRecords = om.readValue(resource, om.getTypeFactory().constructCollectionType(List.class, CourtCaseRecord.class));

        //we want to find strings that are present in at least 5 judgements from at least 2 different months
        HashMap<CharSequence, List<String>> commonSubstrings = new HashMap<>();
        GeneralizedSuffixTree<CourtCaseRecord> myGeneralizedSuffixTree = new GeneralizedSuffixTree<>();
        for (int i = 0; i < courtCaseRecords.size(); i++) {
            myGeneralizedSuffixTree.put(courtCaseRecords.get(i).sentence_hash, courtCaseRecords.get(i));
            if ( i % 100 == 0) {
                log.info("Processed {} cases",i);
            }
        }
        log.info("Finished processing {} cases",courtCaseRecords.size());


        GeneralizedSuffixTree<Integer> lookupTree = new GeneralizedSuffixTree<>();

        AtomicInteger counter = new AtomicInteger(0);
        AtomicInteger counterFiltered = new AtomicInteger(0);
        List<Pair<CharSequence, Collection<CourtCaseRecord>>> longestCommonSubstring = new ArrayList<>();
        myGeneralizedSuffixTree.findAllCommonSubstringsOfSizeInMinKeys(MIN_LENGTH, MIN_JUDGEMENTS, true, true, (sequence, nodes) -> {
            //filter immediately
            //filter to judgements that have keys in different months
            if (nodes.stream().map(CourtCaseRecord::month).distinct().count() > MIN_MONTHS) {
                if (lookupTree.getSearchResults(sequence.toString()).isEmpty()) {
                    lookupTree.put(sequence.toString(), -1);
                    longestCommonSubstring.add(new Pair<>(sequence,nodes));
                    if (counter.incrementAndGet() % 100 == 0) {
                        log.info("Found {} longest common substrings",counter.get());
                    };
                } else {
                    if (counterFiltered.incrementAndGet() % 100 == 0) {
                        log.info("Filtered {} longest common substrings",counterFiltered.get());
                    };
                }
            }
        });

        log.info("Found {} longest common substrings after filter", longestCommonSubstring.size());

        //Filter again, other direction
        GeneralizedSuffixTree<Integer> lookupTree2 = new GeneralizedSuffixTree<>();
        Collections.reverse(longestCommonSubstring);
        Iterator<Pair<CharSequence, Collection<CourtCaseRecord>>> iterator = longestCommonSubstring.iterator();
        while (iterator.hasNext()) {
            Pair<CharSequence, Collection<CourtCaseRecord>> pair = iterator.next();
            if (lookupTree2.getSearchResults(pair.first().toString()).isEmpty()) {
                lookupTree2.put(pair.first().toString(), -1);
            } else {
                iterator.remove();
            }
        }

        log.info("Found {} longest common substrings after filter 2", longestCommonSubstring.size());

        //output to file
        String fullOutput = "";
        for (Pair<CharSequence, Collection<CourtCaseRecord>> pair : longestCommonSubstring) {
            fullOutput += pair.second().stream().findFirst().get().ecli() + ";";
            fullOutput += pair.first() + "\n";
        }

        System.out.println(fullOutput);




        /*solver.add(sentences.get(0));
        solver.add(sentences.get(1));
        List<CharSequence> longestCommonSubstring = solver.getLongestCommonSubstringsForLength(4);
        assertEquals(2, longestCommonSubstring.size());
        assertEquals("asdfasdf",longestCommonSubstring.get(0).toString());
        assertEquals("testte",longestCommonSubstring.get(1).toString());*/
    }

    public record CourtCaseRecord(int case_law_id, String ecli, String url, Date decision_date, int month, String sentence_hash){}
}
