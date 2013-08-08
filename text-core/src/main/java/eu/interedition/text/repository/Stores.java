package eu.interedition.text.repository;

import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.io.CharStreams;
import eu.interedition.text.Segment;
import eu.interedition.text.xml.TextExtractor;
import eu.interedition.text.xml.TextExtractorComponent;

import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class Stores {

    private Stores() {
    }

    public static void toWriter(Store store, long textId, final Segment segment, final Writer target) {
        store.text(textId, segment, new Store.TextCallback() {
            @Override
            public void text(Reader text) throws IOException {
                CharStreams.copy(text, target);
            }
        });
    }

    public static void toWriter(Store store, long textId, Writer target) {
        toWriter(store, textId, null, target);
    }

    public static String toString(Store store, long textId) {
        return toString(store, textId, null);
    }

    public static String toString(Store store, long textId, Segment segment) {
        final StringWriter buf = new StringWriter();
        toWriter(store, textId, segment, buf);
        return buf.toString();
    }

    public static XMLStreamReader xml(Store store, long id, XMLInputFactory xif, XMLStreamReader reader, TextExtractor extractor, StreamFilter... filters) throws IOException, XMLStreamException {
        return xml(store, id, xif, reader, extractor, Arrays.asList(filters));
    }

    public static XMLStreamReader xml(Store store, long id, final XMLInputFactory xif, final XMLStreamReader reader, final TextExtractor extractor, final Iterable<StreamFilter> filters) throws IOException, XMLStreamException {
        try {
            return store.add(id, new Store.TextWriter<XMLStreamReader>() {
                @Override
                public XMLStreamReader write(final Writer writer) throws IOException {
                    try {
                        return extractor.execute(xif, reader, Iterables.concat(filters, Collections.singleton(
                                new TextExtractorComponent() {
                                    @Override
                                    protected int text(String text) {
                                        try {
                                            writer.write(text);
                                            return text.length();
                                        } catch (IOException e) {
                                            throw Throwables.propagate(e);
                                        }
                                    }
                                }
                        )));
                    } catch (Throwable t) {
                        Throwables.propagateIfInstanceOf(Throwables.getRootCause(t), IOException.class);
                        throw Throwables.propagate(t);
                    }
                }
            });
        } catch (Throwable t) {
            Throwables.propagateIfInstanceOf(Throwables.getRootCause(t), XMLStreamException.class);
            throw Throwables.propagate(t);
        }
    }



}
