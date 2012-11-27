package eu.interedition.text.simple;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import eu.interedition.text.Anchor;
import eu.interedition.text.Layer;
import eu.interedition.text.Name;
import eu.interedition.text.TextRange;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class SimpleLayer<T> implements Layer<T> {
    private static final AtomicLong ID_SOURCE = new AtomicLong();

    private final Name name;
    String text;
    private final Set<Anchor> anchors;
    private final T data;
    private final long id;

    public SimpleLayer(Name name, String text, T data, Set<Anchor> anchors) {
        this.name = name;
        this.text = text;
        this.data = data;
        this.anchors = Collections.unmodifiableSet(anchors);
        this.id = ID_SOURCE.addAndGet(1);
    }

    public SimpleLayer(Name name, String text, T data, Anchor... anchors) {
        this(name, text, data, Sets.newHashSet(Arrays.asList(anchors)));
    }

    @Override
    public Name getName() {
        return name;
    }

    @Override
    public Set<Anchor> getAnchors() {
        return anchors;
    }

    @Override
    public T data() {
        return data;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void read(Writer target) throws IOException {
        read(null, target);
    }

    @Override
    public void read(TextRange range, Writer target) throws IOException {
        target.write(range == null ? text : range.apply(text));
    }

    @Override
    public void stream(Consumer consumer) throws IOException {
        consumer.consume(new StringReader(text));
    }

    @Override
    public void stream(TextRange range, Consumer consumer) throws IOException {
        consumer.consume(new StringReader(range.apply(text)));
    }

    @Override
    public String read() {
        return text;
    }

    @Override
    public String read(TextRange range) {
        return range.apply(text);
    }

    @Override
    public SortedMap<TextRange, String> read(SortedSet<TextRange> textRanges) {
        final SortedMap<TextRange, String> result = Maps.newTreeMap();
        for (TextRange textRange : textRanges) {
            result.put(textRange, textRange.apply(text));
        }
        return result;
    }

    @Override
    public long length() {
        return text.length();
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).addValue(name).addValue(Iterables.toString(anchors)).toString();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof SimpleLayer) {
            return id == ((SimpleLayer) obj).id;
        }
        return super.equals(obj);
    }
}
