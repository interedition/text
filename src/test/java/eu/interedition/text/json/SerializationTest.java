package eu.interedition.text.json;

import com.google.common.io.NullOutputStream;
import eu.interedition.text.AbstractTestResourceTest;
import eu.interedition.text.Layer;
import eu.interedition.text.Name;
import eu.interedition.text.Query;
import eu.interedition.text.QueryResultTextStream;
import eu.interedition.text.TextConstants;
import eu.interedition.text.simple.KeyValues;
import java.io.IOException;
import java.io.StringWriter;
import java.util.logging.Level;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class SerializationTest extends AbstractTestResourceTest {

    @Test
    public void simpleSerialization() throws IOException {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new TextModule());

        final StringWriter json = new StringWriter();
        final JsonFactory jf = objectMapper.getJsonFactory();
        final JsonGenerator jg;
        if (LOG.isLoggable(Level.FINE)) {
            jg = jf.createJsonGenerator(json);
            jg.useDefaultPrettyPrinter();
        } else {
            jg = jf.createJsonGenerator(new NullOutputStream());
        }

        final Layer<KeyValues> testText = text();

        jg.writeObject(repository.query(Query.text(testText)));

        jg.writeObject(new QueryResultTextStream<KeyValues>(repository, testText, Query.name(new Name(TextConstants.TEI_NS, "seg"))));

        jg.flush();

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine(json.toString());
        }
    }

}
