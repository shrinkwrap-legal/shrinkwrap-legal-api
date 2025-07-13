package legal.shrinkwrap.api.service;

import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharSequenceNodeFactory;
import com.googlecode.concurrenttrees.solver.LCSubstringSolverForMinLength;
import org.junit.jupiter.api.Test;

import java.util.List;

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

}
