package eu.interedition.text;

import java.net.URI;
import java.util.logging.Logger;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public abstract class AbstractTest {

    protected final Logger LOG = Logger.getLogger(getClass().getName());

    /**
     * Test namespace.
     */
    protected static final URI TEST_NS = URI.create("urn:text-test-ns");


    protected static String escapeNewlines(String str) {
        return str.replaceAll("[\n\r]+", "\\\\n");
    }
}
