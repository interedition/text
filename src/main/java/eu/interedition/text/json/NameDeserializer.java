package eu.interedition.text.json;

import eu.interedition.text.Name;
import java.io.IOException;
import java.net.URI;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;

import static org.codehaus.jackson.JsonToken.END_ARRAY;
import static org.codehaus.jackson.JsonToken.START_ARRAY;
import static org.codehaus.jackson.JsonToken.VALUE_NULL;
import static org.codehaus.jackson.JsonToken.VALUE_STRING;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class NameDeserializer extends JsonDeserializer<Name> {
    @Override
    public Name deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        if (!START_ARRAY.equals(jp.getCurrentToken())) {
            throw new JsonParseException("Name: Expected start of array", jp.getCurrentLocation());
        }

        JsonToken token = jp.nextToken();
        if (!VALUE_STRING.equals(token) && !VALUE_NULL.equals(token)) {
            throw new JsonParseException("Name: Expected string or null as namespace", jp.getCurrentLocation());
        }

        final URI namespace = VALUE_NULL.equals(token) ? null : URI.create(jp.getText());

        token = jp.nextToken();
        if (!VALUE_STRING.equals(token)) {
            throw new JsonParseException("Name: Expected string as local name", jp.getCurrentLocation());
        }
        final Name name = new Name(namespace, jp.getText());

        if (!END_ARRAY.equals(jp.nextToken())) {
            throw new JsonParseException("Name: Expected end of array", jp.getCurrentLocation());
        }

        return name;
    }
}
