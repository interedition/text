package eu.interedition.text.lisp;

import com.google.common.base.Strings;
import eu.interedition.text.AbstractTest;
import java.io.IOException;
import org.junit.Test;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class LispParserTest extends AbstractTest {

    @Test
    public void parse() throws LispParserException, IOException {
        final Expression expr = new LispParser("(or (text 2) (and (name \"w\" \"http://www.tei-c.org/\\\"ns/1.0\") (text 1) (overlaps 1 2) (matches \"\u01ff\")))").expression();
        print(expr, 0);
        System.out.println(expr.toString());
    }

    private void print(Expression expr, int depth) {
        System.out.println(Strings.repeat("\t", depth) + expr.getClass());
        if (expr instanceof ExpressionList) {
            for (Expression contained : ((ExpressionList) expr)) {
                print(contained, depth + 1);
            }

        }
    }
}
