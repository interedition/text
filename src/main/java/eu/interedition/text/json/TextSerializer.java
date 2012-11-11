package eu.interedition.text.json;

import eu.interedition.text.Layer;
import java.io.IOException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class TextSerializer extends JsonSerializer<Layer> {
  private static final String LENGTH_FIELD = "l";

  @Override
  public Class<Layer> handledType() {
    return Layer.class;
  }

  @Override
  public void serialize(Layer value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
    jgen.writeStartObject();
    jgen.writeNumberField(LENGTH_FIELD, value.length());
    jgen.writeEndObject();
  }
}
