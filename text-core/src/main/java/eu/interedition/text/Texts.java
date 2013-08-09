package eu.interedition.text;

import com.google.common.base.Throwables;
import com.google.common.io.CharStreams;
import eu.interedition.text.repository.Store;
import eu.interedition.text.xml.ConverterBuilder;
import eu.interedition.text.repository.StoringConverterListener;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class Texts {

    private Texts() {
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

    public static long xml(final ConverterBuilder extractorBuilder, final Repository repository, final XMLInputFactory xif, final XMLStreamReader reader) throws IOException, XMLStreamException {
        try {
            return repository.execute(new Repository.Transaction<Long>() {
                @Override
                public Long transactional(Store store) {
                    try {
                        final Long textId = repository.textIds().next();
                        xml(extractorBuilder, store, textId, repository.annotationIds(), xif, reader);
                        return textId;
                    } catch (Throwable t) {
                        throw Throwables.propagate(t);
                    }
                }
            });
        } catch (Throwable t) {
            final Throwable rootCause = Throwables.getRootCause(t);
            Throwables.propagateIfInstanceOf(rootCause, XMLStreamException.class);
            Throwables.propagateIfInstanceOf(rootCause, IOException.class);
            throw Throwables.propagate(t);
        }
    }
    public static XMLStreamReader xml(final ConverterBuilder extractorBuilder, final Store store, final long id, final Iterator<Long> annotationIds, final XMLInputFactory xif, final XMLStreamReader reader) throws IOException, XMLStreamException {
        try {
            return store.add(id, new Store.TextWriter<XMLStreamReader>() {
                @Override
                public XMLStreamReader write(final Writer writer) throws IOException {
                    try {
                        return extractorBuilder.build()
                                .add(new StoringConverterListener(store, annotationIds, id, writer))
                                .extract(xif, reader);
                    } catch (Throwable t) {
                        Throwables.propagateIfInstanceOf(Throwables.getRootCause(t), IOException.class);
                        throw Throwables.propagate(t);
                    }
                }
            });
        } catch (Throwable t) {
            final Throwable rootCause = Throwables.getRootCause(t);
            Throwables.propagateIfInstanceOf(rootCause, XMLStreamException.class);
            Throwables.propagateIfInstanceOf(rootCause, IOException.class);
            throw Throwables.propagate(t);
        }
    }
}
