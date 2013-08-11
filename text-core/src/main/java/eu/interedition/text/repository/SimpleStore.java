package eu.interedition.text.repository;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import eu.interedition.text.Annotation;
import eu.interedition.text.AnnotationTarget;
import eu.interedition.text.Segment;
import org.codehaus.jackson.map.ObjectMapper;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class SimpleStore implements Store {

    private final Map<Long, String> texts = Maps.newHashMap();
    private final Map<Long, Annotation> annotations = Maps.newHashMap();
    private final Function<Long, Annotation> annotationResolver = Functions.forMap(annotations);
    private final SetMultimap<Long, Long> text2annotations = HashMultimap.create();
    private final ObjectMapper objectMapper;

    private final TransactionLog txLog = new TransactionLog();

    public SimpleStore(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public TransactionLog txLog() {
        return txLog;
    }

    @Override
    public <R> R contents(ContentsCallback<R> cb) {
        return cb.contents(texts.keySet().iterator());
    }

    @Override
    public <R> R add(long id, TextWriter<R> writer) {
        try {
            final StringWriter stringWriter = new StringWriter();
            final R result = writer.write(stringWriter);
            texts.put(id, stringWriter.toString());
            txLog.textsAdded(id);
            return result;
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void text(long id, TextCallback cb) {
        try {
            cb.text(new StringReader(existingText(id)));
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void text(long id, Segment segment, TextCallback cb) {
        try {
            cb.text(new Segment.Reader(new StringReader(existingText(id)), segment));
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public long textLength(long id) {
        return existingText(id).length();
    }

    @Override
    public SortedMap<Segment, String> segments(long id, SortedSet<Segment> segments) {
        final SortedMap<Segment, String> result = Maps.newTreeMap();
        final String text = existingText(id);
        for (Segment segment : segments) {
            result.put(segment, text.substring(segment.start(), segment.end()));
        }
        return result;
    }

    @Override
    public <R> R textAnnotations(long text, AnnotationsCallback<R> cb) {
        return cb.annotations(Iterables.transform(
                Objects.firstNonNull(text2annotations.get(text), NO_ANNOTATIONS),
                annotationResolver
        ).iterator());
    }

    @Override
    public <R> R annotations(AnnotationsCallback<R> cb, final Iterable<Long> ids) {
        return cb.annotations(new AbstractIterator<Annotation>() {

            final Iterator<Long> idIt = ids.iterator();

            @Override
            protected Annotation computeNext() {
                while (idIt.hasNext()) {
                    final Annotation annotation = annotations.get(idIt.next());
                    if (annotation != null) {
                        return annotation;
                    }
                }
                return endOfData();
            }
        });
    }

    @Override
    public <R> R annotations(AnnotationsCallback<R> cb, Long... ids) {
        return annotations(cb, Arrays.asList(ids));
    }

    @Override
    public <R> R textAnnotations(final long text, Segment segment, AnnotationsCallback<R> cb) {
        final int segmentStart = segment.start();
        final int segmentEnd = segment.end();
        return cb.annotations(textAnnotations(text, new AnnotationsCallback<Iterator<Annotation>>() {
            @Override
            public Iterator<Annotation> annotations(Iterator<Annotation> annotations) {
                return Iterators.filter(annotations, new Predicate<Annotation>() {
                    @Override
                    public boolean apply(@Nullable Annotation input) {
                        return Iterables.any(input.targets(), new Predicate<AnnotationTarget>() {
                            @Override
                            public boolean apply(@Nullable AnnotationTarget input) {
                                return (text == input.text() && (input.end() > segmentStart) && (input.start() < segmentEnd));
                            }
                        });
                    }
                });
            }
        }));
    }

    @Override
    public void annotate(Iterable<Annotation> annotations) {
        for (Annotation annotation : annotations) {
            final long id = annotation.id();
            this.annotations.put(id, annotation);
            for (AnnotationTarget target : annotation.targets()) {
                this.text2annotations.put(target.text(), id);
            }
            txLog.annotationsAdded(id);
        }
    }

    @Override
    public void deleteTexts(Iterable<Long> ids) {
        for (Long id : ids) {
            texts.remove(id);
            txLog.textsRemoved(id);
            deleteAnnotations(text2annotations.get(id));
        }
    }

    @Override
    public void deleteAnnotations(Iterable<Long> ids) {
        for (Long id : ids) {
            annotations.remove(id);
            txLog.annotationsRemoved(id);
        }
    }

    @Override
    public ObjectMapper objectMapper() {
        return objectMapper;
    }

    private String existingText(long id) {
        final String text = texts.get(id);
        if (text == null) {
            throw new IllegalArgumentException(Long.toString(id));
        }
        return text;
    }

    private static final Collection<Long> NO_ANNOTATIONS = Collections.emptySet();
}
