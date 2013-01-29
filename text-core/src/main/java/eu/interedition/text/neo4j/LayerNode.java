package eu.interedition.text.neo4j;

import com.google.common.base.Objects;
import com.google.common.base.Throwables;
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
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class LayerNode<T> implements Layer<T> {

    static final String NAME_NS = "name-ns";
    static final String NAME_LN = "name-ln";

    static final String RANGE_START = "from";
    static final String RANGE_END = "to";

    static final String TEXT = "text";

    public enum Relationships implements RelationshipType {
        ANCHORS, HAS_TEXT
    }

    public final Neo4jTextRepository<T> repository;
    public final Node node;

    public LayerNode(Neo4jTextRepository<T> repository, Node node) {
        this.repository = repository;
        this.node = node;
    }

    @Override
    public long getId() {
        return node.getId();
    }

    @Override
    public Set<Anchor<T>> getAnchors() {
        final Set<Anchor<T>> anchors = Sets.newHashSet();
        for (Relationship anchorRel : node.getRelationships(Direction.OUTGOING, Relationships.ANCHORS)) {
            anchors.add(new Anchor<T>(
                    new LayerNode<T>(repository, anchorRel.getEndNode()),
                    new TextRange(
                            (Long) anchorRel.getProperty(RANGE_START),
                            (Long) anchorRel.getProperty(RANGE_END)
                    )
            ));
        }
        return anchors;
    }

    @Override
    public Name getName() {
        return new Name((String) node.getProperty(NAME_NS, null), (String) node.getProperty(NAME_LN));
    }

    @Override
    public T data() {
        try {
            return repository.data(node);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void read(Writer target) throws IOException {
        target.write(read());
    }

    @Override
    public void read(TextRange range, Writer target) throws IOException {
        target.write(read(range));
    }

    @Override
    public void stream(Consumer consumer) throws IOException {
        stream(null, consumer);
    }

    @Override
    public void stream(TextRange range, Consumer consumer) throws IOException {
        consumer.consume(new StringReader(read(range)));
    }

    @Override
    public String read() throws IOException {
        return (String) node.getProperty(TEXT);
    }

    @Override
    public String read(TextRange range) throws IOException {
        final String text = read();
        return (range == null ? text : range.apply(text));
    }

    @Override
    public SortedMap<TextRange, String> read(SortedSet<TextRange> textRanges) {
        try {
            final SortedMap<TextRange, String> result = Maps.newTreeMap();
            final String text = read();
            for (TextRange range : textRanges) {
                result.put(range, range.apply(text));
            }
            return result;
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public long length() {
        try {
            return read().length();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).addValue(getName()).addValue(Iterables.toString(getAnchors())).toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof LayerNode) {
            LayerNode<?> other = (LayerNode<?>) obj;
            return (node.equals(other.node));
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return node.hashCode();
    }
}
