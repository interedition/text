package eu.interedition.text;

import eu.interedition.text.util.Logging;
import java.net.URI;
import java.util.logging.Logger;
import org.junit.BeforeClass;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public abstract class AbstractTest {

    protected final Logger LOG = Logger.getLogger(getClass().getName());

    /**
     * Test namespace.
     */
    protected static final URI TEST_NS = URI.create("urn:text-test-ns");


    @BeforeClass
    public static void init() {
        Logging.configureLogging();
    }

    protected static String escapeNewlines(String str) {
        return str.replaceAll("[\n\r]+", "\\\\n");
    }
}
