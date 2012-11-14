package eu.interedition.text.json;

import eu.interedition.text.Anchor;
import eu.interedition.text.Layer;
import eu.interedition.text.Text;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class AnchorSerializer extends JsonSerializer<Anchor> {

    @Override
    public Class<Anchor> handledType() {
        return Anchor.class;
    }

    @Override
    public void serialize(Anchor value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {

        jgen.writeStartObject();

        jgen.writeObjectFieldStart("t");
        final Text text = value.getText();
        if (text instanceof Layer) {
            Layer<?> target = (Layer<?>) text;
            jgen.writeObjectField("name", target.getName());
        }
        try {
            jgen.writeObjectField("id", text.getClass().getMethod("getId").invoke(text));
        } catch (NoSuchMethodException e) {
        } catch (InvocationTargetException e) {
        } catch (IllegalAccessException e) {
        }
        jgen.writeEndObject();

        jgen.writeObjectField("range", value.getRange());

        jgen.writeEndObject();
    }
}
