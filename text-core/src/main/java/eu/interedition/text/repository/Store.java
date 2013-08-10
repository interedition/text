package eu.interedition.text.repository;

import eu.interedition.text.Annotation;
import eu.interedition.text.Segment;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public interface Store {

    <R> R contents(ContentsCallback<R> cb);

    <R> R add(long id, TextWriter<R> writer);

    void text(long id, TextCallback cb);

    void text(long id, Segment segment, TextCallback cb);

    long textLength(long id);

    SortedMap<Segment, String> segments(long id, SortedSet<Segment> segments);

    <R> R annotations(long text, AnnotationsCallback<R> cb);

    <R> R annotations(long text, Segment segment, AnnotationsCallback<R> cb);

    void annotate(Iterable<Annotation> annotations);

    void deleteTexts(Iterable<Long> ids);

    void deleteAnnotations(Iterable<Long> ids);

    ObjectMapper objectMapper();

    /**
    * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
    */
    interface TextCallback {

        void text(Reader text) throws IOException;

    }

    /**
     * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
     */
    interface TextWriter<R> {

        R write(Writer writer) throws IOException;

    }

    /**
     * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
     */
    interface AnnotationsCallback<R> {

        R annotations(Iterator<Annotation> annotations);
    }

    /**
     * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
     */
    interface ContentsCallback<R> {

        R contents(Iterator<Long> ids);
    }

}
