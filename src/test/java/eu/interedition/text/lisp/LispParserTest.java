package eu.interedition.text.lisp;

import eu.interedition.text.AbstractTest;
import java.io.IOException;
import org.junit.Test;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class LispParserTest extends AbstractTest {

    @Test
    public void parse() throws LispParserException, IOException {
        System.out.println(new LispParser("(or (text 2) (and (name \"w\" \"http://www.tei-c.org/ns/1.0\") (text 1) (overlaps 1 2)))").expression().toString());
    }
}
