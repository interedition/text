package eu.interedition.text.json;

import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import eu.interedition.text.Layer;
import eu.interedition.text.TextRange;
import eu.interedition.text.TextStream;
import java.io.IOException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class TextStreamSerializer extends JsonSerializer<TextStream> {

    @Override
    public Class<TextStream> handledType() {
        return TextStream.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void serialize(TextStream value, final JsonGenerator jgen, SerializerProvider provider) throws IOException {
        try {
            value.stream(new TextStream.ExceptionPropagatingListenerAdapter() {
                @Override
                protected void doStart(long contentLength) throws Exception {
                    jgen.writeStartArray();
                    jgen.writeStartObject();
                    jgen.writeStringField("event", "startStream");
                    jgen.writeNumberField("contentLength", contentLength);
                    jgen.writeEndObject();
                }

                @Override
                protected void doStart(long offset, Iterable layers) throws Exception {
                    jgen.writeStartObject();
                    jgen.writeStringField("event", "start");
                    jgen.writeNumberField("offset", offset);
                    jgen.writeArrayFieldStart("layers");
                    for (Layer layer : Iterables.filter(layers, Layer.class)) {
                        jgen.writeObject(layer);
                    }
                    jgen.writeEndArray();
                    jgen.writeEndObject();
                }

                @Override
                protected void doEnd(long offset, Iterable layers) throws Exception {
                    jgen.writeStartObject();
                    jgen.writeStringField("event", "end");
                    jgen.writeNumberField("offset", offset);
                    jgen.writeArrayFieldStart("layers");
                    for (Layer layer : Iterables.filter(layers, Layer.class)) {
                        jgen.writeObject(layer);
                    }
                    jgen.writeEndArray();
                    jgen.writeEndObject();
                }

                @Override
                protected void doText(TextRange r, String text) throws Exception {
                    jgen.writeStartObject();
                    jgen.writeStringField("event", "text");
                    jgen.writeObjectField("range", r);
                    jgen.writeStringField("text", text);
                    jgen.writeEndObject();
                }

                @Override
                protected void doEnd() throws Exception {
                    jgen.writeStartObject();
                    jgen.writeStringField("event", "endStream");
                    jgen.writeEndObject();
                    jgen.writeEndArray();
                }
            });
        } catch (Throwable t) {
            Throwables.propagateIfInstanceOf(Throwables.getRootCause(t), IOException.class);
            throw Throwables.propagate(t);
        }
    }
}
