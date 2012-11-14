package eu.interedition.text.json;

import eu.interedition.text.Layer;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
class LayerSerializer extends JsonSerializer<Layer> {

    @Override
    public Class<Layer> handledType() {
        return Layer.class;
    }

    @Override
    public void serialize(Layer value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();

        jgen.writeObjectField("name", value.getName());
        jgen.writeObjectField("anchors", value.getAnchors());
        jgen.writeObjectField("data", value.data());
        try {
            jgen.writeObjectField("id", value.getClass().getMethod("getId").invoke(value));
        } catch (NoSuchMethodException e) {
        } catch (InvocationTargetException e) {
        } catch (IllegalAccessException e) {
        }

        jgen.writeEndObject();
    }
}
