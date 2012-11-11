package eu.interedition.text.json;

import eu.interedition.text.Layer;
import eu.interedition.text.Name;
import java.io.IOException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class AnnotationSerializer extends JsonSerializer<Layer> {

    public static final String NAME_FIELD = "n";
    public static final String TARGET_FIELD = "t";
    private static final String DATA_FIELD = "d";

    @Override
    public Class<Layer> handledType() {
        return Layer.class;
    }

    @Override
    public void serialize(Layer value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();

        final Name name = value.getName();
        jgen.writeFieldName(NAME_FIELD);
        jgen.writeObject(name);

        jgen.writeObjectField(TARGET_FIELD, value.getAnchors());
        jgen.writeFieldName(DATA_FIELD);

        jgen.writeEndObject();
    }
}
