package eu.interedition.text.json;

import com.google.common.collect.Maps;
import com.google.common.io.NullOutputStream;
import eu.interedition.text.AbstractTestResourceTest;
import eu.interedition.text.Query;
import eu.interedition.text.TextConstants;
import eu.interedition.text.TextRange;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.Map;
import java.util.logging.Level;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class JSONSerializerTest extends AbstractTestResourceTest {

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

        JSONSerializer.serialize(jg, repository, text(), new JSONSerializerConfiguration() {
            @Override
            public TextRange getRange() {
                return null;
            }

            @Override
            public Map<String, URI> getNamespaceMappings() {
                Map<String, URI> nsMap = Maps.newHashMap();
                nsMap.put("tei", TextConstants.TEI_NS);
                nsMap.put("xml", TextConstants.XML_NS_URI);
                return nsMap;
            }

            @Override
            public Query getQuery() {
                return Query.any();
            }
        });
        jg.flush();

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine(json.toString());
        }
    }

}
