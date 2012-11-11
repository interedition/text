package eu.interedition.text.h2;

import eu.interedition.text.AbstractTest;
import eu.interedition.text.Name;
import eu.interedition.text.Query;
import eu.interedition.text.TextConstants;
import org.junit.Test;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class H2QueryTest extends AbstractTest {

    @Test
    public void sql() {
        LOG.fine(new H2Query().sql(Query.name(new Name(TextConstants.INTEREDITION_NS_URI, "test"))));
    }
}
