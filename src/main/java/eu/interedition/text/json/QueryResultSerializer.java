package eu.interedition.text.json;

import com.google.common.collect.Iterables;
import eu.interedition.text.Layer;
import eu.interedition.text.QueryResult;
import java.io.IOException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class QueryResultSerializer extends JsonSerializer<QueryResult> {

    @Override
    public Class<QueryResult> handledType() {
        return QueryResult.class;
    }

    @Override
    public void serialize(QueryResult value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartArray();
        for (Layer layer : Iterables.filter(value, Layer.class)) {
            jgen.writeObject(layer);
        }
        jgen.writeEndArray();
    }
}
