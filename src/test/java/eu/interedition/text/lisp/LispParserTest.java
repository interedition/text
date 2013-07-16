package eu.interedition.text.lisp;

import com.google.common.collect.Iterables;
import eu.interedition.text.AbstractTestResourceTest;
import eu.interedition.text.AbstractTextTest;
import eu.interedition.text.Layer;
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
public class LispParserTest extends AbstractTestResourceTest {

    @Test
    public void parse() throws LispParserException, IOException {
        final QueryParser<KeyValues> qp = new QueryParser<KeyValues>(repository);
        new QueryResultTextStream<KeyValues>(repository,
                text(),
                qp.parse("(and (name \"supplied\") (overlaps 0 1000))")
        ).stream(new TextStream.ListenerAdapter<KeyValues>() {

            @Override
            public void start(long offset, Iterable<Layer<KeyValues>> layers) {
                System.out.println(Iterables.toString(layers));
            }

        });

    }
}
