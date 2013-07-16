package eu.interedition.text.json;

import eu.interedition.text.Name;
import java.io.IOException;
import java.net.URI;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class NameSerializer extends JsonSerializer<Name> {

    @Override
    public Class<Name> handledType() {
        return Name.class;
    }

    @Override
    public void serialize(Name value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
        jgen.writeStartArray();

        final URI ns = value.getNamespace();
        if (ns == null) {
            jgen.writeNull();
        } else {
            jgen.writeString(ns.toString());
        }

        jgen.writeString(value.getLocalName());

        jgen.writeEndArray();
    }
}
