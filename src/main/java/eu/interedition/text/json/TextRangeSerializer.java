package eu.interedition.text.json;

import eu.interedition.text.TextRange;
import java.io.IOException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class TextRangeSerializer extends JsonSerializer<TextRange> {

    @Override
    public Class<TextRange> handledType() {
        return TextRange.class;
    }

    @Override
    public void serialize(TextRange value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
        jgen.writeStartArray();
        jgen.writeNumber(value.getStart());
        jgen.writeNumber(value.getEnd());
        jgen.writeEndArray();
    }
}
