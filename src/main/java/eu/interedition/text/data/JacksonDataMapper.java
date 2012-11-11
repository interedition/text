package eu.interedition.text.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class JacksonDataMapper<T> implements DataMapper<T> {

    private final ObjectMapper objectMapper;

    public JacksonDataMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

    }

    @Override
    public void serialize(T data, OutputStream stream) throws IOException {
        final JsonGenerator jg = this.objectMapper.getJsonFactory().createJsonGenerator(stream);
        jg.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        try {
            jg.writeObject(data);
        } finally {
            jg.close();
        }
    }

    @Override
    public T deserialize(InputStream stream, Class<T> type) throws IOException {
        final JsonParser jp = this.objectMapper.getJsonFactory().createJsonParser(stream);
        jp.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
        try {
            return jp.readValueAs(type);
        } finally {
            jp.close();
        }

    }
}
