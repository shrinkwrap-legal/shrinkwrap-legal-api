package legal.shrinkwrap.api.service;

import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharSequenceNodeFactory;
import com.googlecode.concurrenttrees.solver.LCSubstringSolverForMinLength;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CommonSubstringSolverTest {
    @Test
    public void testFindCommonSubstring() {
        String strA = "usbikmerbgfsmknoöeftesttewabaiojvadawevbsfewafadkjldasdfasdffsajklöbfgnvmklöwavcmonköadslköjfasdjklfsfdn";
        String strB = "sfnklmöfaomnöbvaiomnöopkewasklmöasdfasdföladfskdjviuoreaofdsjldksfjkslvömioaewmtestteckasldfkjaieomlasdfasdf";

        LCSubstringSolverForMinLength solver = new LCSubstringSolverForMinLength(new DefaultCharSequenceNodeFactory());
        solver.add(strA);
        solver.add(strB);
        List<CharSequence> longestCommonSubstring = solver.getLongestCommonSubstringsForLength(4);
        assertEquals(2, longestCommonSubstring.size());
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
        for (int i = 0; i < sentences.size(); i++) {
            System.out.println("sentence " + i + " : " + sentences.get(i));
            for (int j = i+1; j < sentences.size(); j++) {
                if (sentences.get(i).length() < 40 || sentences.get(j).length() < 40) {
                    continue;
                }
                LCSubstringSolverForMinLength solver = new LCSubstringSolverForMinLength(new DefaultCharSequenceNodeFactory());
                solver.add(sentences.get(i));
                solver.add(sentences.get(j));
                List<CharSequence> longestCommonSubstring = solver.getLongestCommonSubstringsForLength(15);
                if (longestCommonSubstring.size() > 1) {
                    for (CharSequence substring : longestCommonSubstring) {
                        if (!commonSubstrings.containsKey(substring)) {
                            commonSubstrings.put(substring, new ArrayList<>());
                        }
                        commonSubstrings.get(substring).add(info.get(i).split(",",3)[1]);
                        commonSubstrings.get(substring).add(info.get(j).split(",",3)[1]);
                    }
                }
            }

        }
        System.out.println(commonSubstrings);

        /*solver.add(sentences.get(0));
        solver.add(sentences.get(1));
        List<CharSequence> longestCommonSubstring = solver.getLongestCommonSubstringsForLength(4);
        assertEquals(2, longestCommonSubstring.size());
        assertEquals("asdfasdf",longestCommonSubstring.get(0).toString());
        assertEquals("testte",longestCommonSubstring.get(1).toString());*/
    }

}
