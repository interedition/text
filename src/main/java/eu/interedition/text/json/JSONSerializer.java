package eu.interedition.text.json;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;
import eu.interedition.text.Layer;
import eu.interedition.text.Name;
import eu.interedition.text.Query;
import eu.interedition.text.TextRange;
import eu.interedition.text.TextRepository;
import java.io.IOException;
import java.net.URI;
import java.util.SortedSet;
import org.codehaus.jackson.JsonGenerator;

import static eu.interedition.text.Query.and;
import static eu.interedition.text.Query.rangeOverlap;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class JSONSerializer {

    public static final String TEXT_FIELD = "t";
    public static final String TEXT_LENGTH_FIELD = "l";
    public static final String ANNOTATIONS_FIELD = "a";
    public static final String NAMES_FIELD = "n";

    public static void serialize(final JsonGenerator jgen, TextRepository<?> repository, Layer<?> layer, final JSONSerializerConfiguration config) throws IOException {
        final TextRange range = config.getRange();

        jgen.writeStartObject();

        final BiMap<String, URI> nsMap = HashBiMap.create(config.getNamespaceMappings());
        final BiMap<URI, String> prefixMap = (nsMap == null ? null : nsMap.inverse());

        final SortedSet<Name> names = Sets.newTreeSet();
        final Query.OperatorQuery criterion = and(config.getQuery());
        if (range != null) {
            criterion.add(rangeOverlap(range));
        }
        jgen.writeArrayFieldStart(ANNOTATIONS_FIELD);
        for (Layer annotation : repository.query(criterion)) {
            jgen.writeObject(annotation);
            names.add(annotation.getName());
        }
        jgen.writeEndArray();

        if (!names.isEmpty()) {
            jgen.writeArrayFieldStart(NAMES_FIELD);
            for (Name n : names) {
                NameSerializer.serialize(n, jgen, prefixMap);
            }
            jgen.writeEndArray();
        }

        jgen.writeNumberField(TEXT_LENGTH_FIELD, layer.length());
        jgen.writeStringField(TEXT_FIELD, CharStreams.toString(layer.read(range == null ? new TextRange(0, layer.length()) : range)));

        jgen.writeEndObject();
    }
}
