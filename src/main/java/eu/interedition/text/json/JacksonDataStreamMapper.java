package eu.interedition.text.json;

import eu.interedition.text.h2.DataStreamMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class JacksonDataStreamMapper<T> implements DataStreamMapper<T> {

    private final ObjectMapper objectMapper;

    public JacksonDataStreamMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

    }

    @Override
    public void write(T data, OutputStream stream) throws IOException {
        final JsonGenerator jg = this.objectMapper.getJsonFactory().createJsonGenerator(stream);
        jg.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        try {
            jg.writeObject(data);
        } finally {
            jg.close();
        }
    }

    @Override
    public T read(InputStream stream, Class<T> type) throws IOException {
        final JsonParser jp = this.objectMapper.getJsonFactory().createJsonParser(stream);
        jp.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
        try {
            return jp.readValueAs(type);
        } finally {
            jp.close();
        }
    }
}
