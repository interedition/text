package eu.interedition.text;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import eu.interedition.text.simple.SimpleTextRepository;
import eu.interedition.text.util.AutoCloseables;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import javax.annotation.Nullable;

import static eu.interedition.text.Query.and;
import static eu.interedition.text.Query.any;
import static eu.interedition.text.Query.rangeOverlap;
import static eu.interedition.text.Query.text;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class QueryResultTextStream<T> implements TextStream<T> {

    private final TextRepository<T> repository;
    private final Text text;
    private final Query query;
    private final long pageSize;

    private QueryResultTextStream(TextRepository<T> repository, Text text, Query query, long pageSize) {
        this.repository = repository;
        this.text = text;
        this.query = query;
        this.pageSize = pageSize;
    }

    public QueryResultTextStream(TextRepository<T> repository, Text text, Query query) {
        this(repository, text, query, Long.MAX_VALUE);
    }

    public QueryResultTextStream(TextRepository<T> repository, Text text) {
        this(repository, text, any());
    }

    @Override
    public void stream(final Listener<T> listener) throws IOException {
        final long contentLength = text.length();
        text.stream(new Text.Consumer() {
            @Override
            public void consume(Reader textReader) throws IOException {
                final SortedMap<Long, Set<Layer<T>>> starts = Maps.newTreeMap();
                final SortedMap<Long, Set<Layer<T>>> ends = Maps.newTreeMap();

                long offset = 0;
                long next = 0;
                long pageEnd = 0;

                listener.start(contentLength);

                final Set<Layer<T>> annotationData = Sets.newHashSet();
                while (true) {
                    if ((offset % pageSize) == 0) {
                        pageEnd = Math.min(offset + pageSize, contentLength);
                        final TextRange pageRange = new TextRange(offset, pageEnd);
                        final QueryResult<T> page = repository.query(and(query, text(text), rangeOverlap(pageRange)));
                        try {
                            for (Layer<T> a : page) {
                                for (Anchor anchor : a.getAnchors()) {
                                    if (!text.equals(anchor.getText())) {
                                        continue;
                                    }
                                    final TextRange range = anchor.getRange();
                                    final long start = range.getStart();
                                    final long end = range.getEnd();
                                    if (start >= offset) {
                                        Set<Layer<T>> starting = starts.get(start);
                                        if (starting == null) {
                                            starts.put(start, starting = Sets.newHashSet());
                                        }
                                        starting.add(a);
                                        annotationData.add(a);
                                    }
                                    if (end <= pageEnd) {
                                        Set<Layer<T>> ending = ends.get(end);
                                        if (ending == null) {
                                            ends.put(end, ending = Sets.newHashSet());
                                        }
                                        ending.add(a);
                                        annotationData.add(a);
                                    }
                                }
                            }
                        } finally {
                            AutoCloseables.closeQuietly(page);
                        }

                        next = Math.min(starts.isEmpty() ? contentLength : starts.firstKey(), ends.isEmpty() ? contentLength : ends.firstKey());
                    }

                    if (offset == next) {
                        final Set<Layer<T>> startEvents = (!starts.isEmpty() && offset == starts.firstKey() ? starts.remove(starts.firstKey()) : Sets.<Layer<T>>newHashSet());
                        final Set<Layer<T>> endEvents = (!ends.isEmpty() && offset == ends.firstKey() ? ends.remove(ends.firstKey()) : Sets.<Layer<T>>newHashSet());

                        final Set<Layer<T>> emptyEvents = Sets.newHashSet(Sets.filter(endEvents, emptyIn(text)));
                        endEvents.removeAll(emptyEvents);

                        if (!endEvents.isEmpty()) listener.end(offset, substract(annotationData, endEvents, true));
                        if (!startEvents.isEmpty())
                            listener.start(offset, substract(annotationData, startEvents, false));
                        if (!emptyEvents.isEmpty()) listener.end(offset, substract(annotationData, emptyEvents, true));

                        next = Math.min(starts.isEmpty() ? contentLength : starts.firstKey(), ends.isEmpty() ? contentLength : ends.firstKey());
                    }

                    if (offset == contentLength) {
                        break;
                    }

                    final long readTo = Math.min(pageEnd, next);
                    if (offset < readTo) {
                        final char[] currentText = new char[(int) (readTo - offset)];
                        int read = textReader.read(currentText);
                        if (read > 0) {
                            listener.text(new TextRange(offset, offset + read), new String(currentText, 0, read));
                            offset += read;
                        }
                    }
                }

                listener.end();
            }
        });
    }


    private Predicate<Layer<T>> emptyIn(final Text text) {
        return new SimpleTextRepository.AnyAnchorPredicate<T>(new Predicate<Anchor>() {
            @Override
            public boolean apply(@Nullable Anchor input) {
                return input.getRange().length() == 0 && text.equals(input.getText());
            }
        });
    }

    private static <T> Iterable<Layer<T>> substract(Iterable<Layer<T>> from, Set<Layer<T>> selector, boolean remove) {
        final List<Layer<T>> filtered = Lists.newArrayList();
        for (Iterator<Layer<T>> it = from.iterator(); it.hasNext(); ) {
            final Layer<T> annotation = it.next();
            if (selector.contains(annotation)) {
                filtered.add(annotation);
                if (remove) {
                    it.remove();
                }
            }
        }
        return filtered;
    }


}
