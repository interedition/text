package eu.interedition.text.neo4j;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;
import eu.interedition.text.Anchor;
import eu.interedition.text.Layer;
import eu.interedition.text.Name;
import eu.interedition.text.Query;
import eu.interedition.text.QueryResult;
import eu.interedition.text.Text;
import eu.interedition.text.TextRange;
import eu.interedition.text.TextRepository;
import eu.interedition.text.util.UpdateSupport;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;

import static eu.interedition.text.neo4j.LayerNode.Relationships.ANCHORS;
import static eu.interedition.text.neo4j.LayerNode.Relationships.PRIMES;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class Neo4jTextRepository<T> implements TextRepository<T>, UpdateSupport<T> {

    private final Class<T> dataType;
    private final DataNodeMapper<T> dataNodeMapper;
    private final GraphDatabaseService db;
    private final boolean transactional;

    public Neo4jTextRepository(Class<T> dataType, DataNodeMapper<T> dataNodeMapper, GraphDatabaseService db, boolean transactional) {
        this.dataType = dataType;
        this.dataNodeMapper = dataNodeMapper;
        this.db = db;
        this.transactional = transactional;
    }

    @Override
    public Layer<T> findByIdentifier(long id) {
        try {
            return new LayerNode<T>(this, db.getNodeById(id));
        } catch (NotFoundException e) {
            return null;
        }
    }

    @Override
    public QueryResult<T> query(Query query) {
        final Transaction tx = begin();
        try {
            return new QueryResult<T>() {
                @Override
                public void close() throws IOException {
                }

                @Override
                public Iterator<Layer<T>> iterator() {
                    return new AbstractIterator<Layer<T>>() {
                        @Override
                        protected Layer<T> computeNext() {
                            return endOfData();
                        }
                    };
                }
            };
        } finally {
            commit(tx);
        }
    }

    @Override
    public Layer<T> add(Name name, Reader text, T data, Set<Anchor> anchors) throws IOException {
        final Transaction tx = begin();
        try {
            final Node node = db.createNode();

            final URI namespace = name.getNamespace();
            if (namespace != null) {
                node.setProperty(LayerNode.NAME_NS, name.getNamespace().toString());
            }
            node.setProperty(LayerNode.NAME_LN, name.getLocalName());
            node.setProperty(LayerNode.TEXT, CharStreams.toString(text));
            dataNodeMapper.write(data, node);

            boolean baseLayer = true;
            for (Anchor anchor : anchors) {
                final Text anchorText = anchor.getText();
                if (anchorText instanceof LayerNode) {
                    final Relationship anchorRel = node.createRelationshipTo(((LayerNode) anchorText).node, ANCHORS);

                    final TextRange anchorRange = anchor.getRange();
                    anchorRel.setProperty(LayerNode.RANGE_START, anchorRange.getStart());
                    anchorRel.setProperty(LayerNode.RANGE_END, anchorRange.getEnd());

                    baseLayer = false;
                }
            }
            if (baseLayer) {
                db.getReferenceNode().createRelationshipTo(node, PRIMES);
            }
            return new LayerNode<T>(this, node);
        } finally {
            commit(tx);
        }
    }

    @Override
    public Layer<T> add(Name name, Reader text, T data, Anchor... anchors) throws IOException {
        return add(name, text, data, Sets.newHashSet(Arrays.asList(anchors)));
    }

    @Override
    public void delete(Iterable<Layer<T>> layers) {
        final Transaction tx = begin();
        try {
            final Set<Node> nodes = Sets.newHashSet();
            for (LayerNode<?> toDelete : Iterables.filter(layers, LayerNode.class)) {
                nodes.add(toDelete.node);
                for (Relationship anchorRel : TRANSITIVE_ANCHORING.traverse(toDelete.node).relationships()) {
                    nodes.add(anchorRel.getStartNode());
                    anchorRel.delete();
                }
            }
            for (Node node : nodes) {
                for (Relationship primeRel : node.getRelationships(PRIMES)) {
                    primeRel.delete();
                }
                node.delete();
            }
        } finally {
            commit(tx);
        }
    }

    private void commit(Transaction tx) {
        if (tx != null) {
            try {
                tx.success();
            } finally {
                tx.finish();
            }
        }
    }

    private Transaction begin() {
        return (transactional ? db.beginTx() : null);
    }

    @Override
    public void updateText(Layer<T> target, Reader text) throws IOException {
        if (target instanceof LayerNode) {
            ((LayerNode<T>) target).node.setProperty(LayerNode.TEXT, CharStreams.toString(text));
        }
    }

    public T data(Node source) throws IOException {
        return dataNodeMapper.read(source, dataType);
    }

    private static final TraversalDescription TRANSITIVE_ANCHORING = Traversal.description()
            .uniqueness(Uniqueness.RELATIONSHIP_GLOBAL)
            .relationships(ANCHORS, Direction.INCOMING);

}
