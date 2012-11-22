package eu.interedition.text.lisp;

import eu.interedition.text.AbstractTextTest;
import eu.interedition.text.QueryResultTextStream;
import eu.interedition.text.TextConstants;
import eu.interedition.text.TextRange;
import eu.interedition.text.TextStream;
import eu.interedition.text.simple.KeyValues;
import java.io.IOException;
import org.junit.Test;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class LispParserTest extends AbstractTextTest {

    @Test
    public void parse() throws LispParserException, IOException {
        final QueryParser<KeyValues> qp = new QueryParser<KeyValues>(repository);
        new QueryResultTextStream<KeyValues>(repository,
                testText(),
                qp.parse("(and (name \"w\" \"" + TextConstants.TEI_NS.toString() + "\") (overlaps 0 100))")
        ).stream(new TextStream.ListenerAdapter<KeyValues>() {
            @Override
            public void text(TextRange r, String text) {
                System.out.println(text);
            }


        });

    }
}
