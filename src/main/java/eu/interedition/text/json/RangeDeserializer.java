package eu.interedition.text.json;

import eu.interedition.text.TextRange;
import java.io.IOException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;

import static org.codehaus.jackson.JsonToken.END_ARRAY;
import static org.codehaus.jackson.JsonToken.START_ARRAY;
import static org.codehaus.jackson.JsonToken.VALUE_NUMBER_INT;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class RangeDeserializer extends JsonDeserializer<TextRange> {
    @Override
    public TextRange deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        if (!START_ARRAY.equals(jp.getCurrentToken())) {
            throw new JsonParseException("TextRange: Expected start of array", jp.getCurrentLocation());
        }

        JsonToken token = jp.nextToken();
        if (!VALUE_NUMBER_INT.equals(token)) {
            throw new JsonParseException("TextRange: Expected number as start of range", jp.getCurrentLocation());
        }

        final long start = jp.getValueAsLong();

        token = jp.nextToken();
        if (!VALUE_NUMBER_INT.equals(token)) {
            throw new JsonParseException("TextRange: Expected number as end of range", jp.getCurrentLocation());
        }
        final TextRange range = new TextRange(start, jp.getValueAsLong());

        if (!END_ARRAY.equals(jp.nextToken())) {
            throw new JsonParseException("TextRange: Expected end of array", jp.getCurrentLocation());
        }

        return range;
    }
}
