package eu.interedition.text.json;

import eu.interedition.text.neo4j.DataNodeMapper;
import java.io.IOException;
import java.io.StringWriter;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.graphdb.Node;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class JacksonDataNodeMapper<T> implements DataNodeMapper<T> {
    private static final String DATA = "data";

    private final ObjectMapper objectMapper;

    public JacksonDataNodeMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public T read(Node source, Class<T> type) throws IOException {
        final String data = (String) source.getProperty(DATA, null);
        if (data == null) {
            return null;
        }
        final JsonParser jp = this.objectMapper.getJsonFactory().createJsonParser(data);
        try {
            return jp.readValueAs(type);
        } finally {
            jp.close();
        }
    }

    @Override
    public void write(T data, Node target) throws IOException {
        if (data == null) {
            target.removeProperty(DATA);
        }
        final StringWriter dataBuf = new StringWriter();
        final JsonGenerator jg = this.objectMapper.getJsonFactory().createJsonGenerator(dataBuf);
        try {
            jg.writeObject(data);
        } finally {
            jg.close();
        }
        target.setProperty(DATA, dataBuf.toString());
    }
}
