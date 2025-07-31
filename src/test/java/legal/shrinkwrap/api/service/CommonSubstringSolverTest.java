package legal.shrinkwrap.api.service;


import net.mezzdev.suffixtree.GeneralizedSuffixTree;
import net.mezzdev.suffixtree.Pair;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CommonSubstringSolverTest {
    @Test
    public void testFindCommonSubstring() {
        String strA = "usbikmerbgfsmknoöeftesttewabaiojvadawevbsfewafadkjldasdfasdffsajklöbfgnvmklöwavcmonköadslköjfasdjklfsfdn";
        String strB = "sfnklmöfaomnöbvaiomnöopkewasklmöasdfasdföladfskdjviuoreaofdsjldksfjkslvömioaewmtestteckasldfkjaieomlasdfasdf";

        List<String> longestCommonSubstring = new ArrayList<>();
        GeneralizedSuffixTree<Integer> solver = new GeneralizedSuffixTree<>();
        solver.put(strA, 1);
        solver.put(strB, 2);
        solver.findAllCommonSubstringsOfSizeInMinKeys(6,2,false, true, (c, i) -> {
            longestCommonSubstring.add(c.toString());
        });
        assertEquals(4, longestCommonSubstring.size());
        assertEquals("asdfasdf",longestCommonSubstring.get(0).toString());
        assertEquals("testte",longestCommonSubstring.get(1).toString());
    }


    @Test
    public void testFindCommonSubstringWithData() throws IOException, URISyntaxException {
        URL resource = getClass().getClassLoader().getResource("commonSubstringTest.csv");
        assertNotNull(resource, "Resource not found!");
        Path path = Paths.get(resource.toURI());
        List<String> info =  Files.readAllLines(path);
        List<String> sentences = info.stream().map(s -> s.split(",",3)[2]).toList();

        HashMap<CharSequence, List<String>> commonSubstrings = new HashMap<>();
        GeneralizedSuffixTree<Integer> myGeneralizedSuffixTree = new GeneralizedSuffixTree<>();
        for (int i = 0; i < 100; i++) {
            myGeneralizedSuffixTree.put(sentences.get(i), i);
            System.out.println(i);
        }
        AtomicInteger counter = new AtomicInteger(0);
        List<Pair<CharSequence, Collection<Integer>>> longestCommonSubstring = new ArrayList<>();
                myGeneralizedSuffixTree.findAllCommonSubstringsOfSizeInMinKeys(10, 3, true, true, (sequence, nodes) -> {
                    longestCommonSubstring.add(new Pair<CharSequence, Collection<Integer>>(sequence,nodes));
                    System.out.println(counter.incrementAndGet() + ": " + nodes + " > " + sequence);
                });
        System.out.println(longestCommonSubstring.size());

        /*solver.add(sentences.get(0));
        solver.add(sentences.get(1));
        List<CharSequence> longestCommonSubstring = solver.getLongestCommonSubstringsForLength(4);
        assertEquals(2, longestCommonSubstring.size());
        assertEquals("asdfasdf",longestCommonSubstring.get(0).toString());
        assertEquals("testte",longestCommonSubstring.get(1).toString());*/
    }

}
