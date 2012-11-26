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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.RelationshipIndex;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.index.lucene.QueryContext;
import org.neo4j.index.lucene.ValueContext;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;

import static eu.interedition.text.neo4j.LayerNode.Relationships.ANCHORS;
import static eu.interedition.text.neo4j.LayerNode.Relationships.HAS_TEXT;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class Neo4jTextRepository<T> implements TextRepository<T>, UpdateSupport<T> {
    private final Logger LOG = Logger.getLogger(getClass().getName());

    private final Neo4jIndexQuery indexQuery = new Neo4jIndexQuery();
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
    public QueryResult<T> query(final Query query) {
        final Transaction tx = begin();
        return new QueryResult<T>() {
            @Override
            public void close() throws IOException {
                commit(tx);
            }

            @Override
            public Iterator<Layer<T>> iterator() {
                final org.apache.lucene.search.Query luceneQuery = indexQuery.build(query);
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "Lucene Query: {0}", luceneQuery);
                }
                final Iterator<Relationship> relationships = index().query(
                        new QueryContext(luceneQuery).sort(new Sort(new SortField("id", SortField.LONG)))
                ).iterator();

                return new AbstractIterator<Layer<T>>() {
                    long last = 0;

                    @Override
                    protected Layer<T> computeNext() {
                        while (relationships.hasNext()) {
                            final Node layerNode = relationships.next().getStartNode();
                            if (layerNode.getId() != last) {
                                last = layerNode.getId();
                                return new LayerNode<T>(Neo4jTextRepository.this, layerNode);
                            }
                        }
                        return endOfData();
                    }
                };
            }
        };
    }

    @Override
    public Layer<T> add(Name name, Reader text, T data, Set<Anchor> anchors) throws IOException {
        final Transaction tx = begin();
        try {
            final Node node = db.createNode();

            final URI namespace = name.getNamespace();
            final String localName = name.getLocalName();
            final String ns = (namespace == null ? "" : namespace.toString());
            if (!ns.isEmpty()) {
                node.setProperty(LayerNode.NAME_NS, ns);
            }
            node.setProperty(LayerNode.NAME_LN, localName);
            node.setProperty(LayerNode.TEXT, CharStreams.toString(text));
            dataNodeMapper.write(data, node);

            final RelationshipIndex index = index();

            for (Anchor anchor : anchors) {
                final Text anchorText = anchor.getText();
                if (anchorText instanceof LayerNode) {
                    final Node anchorTextNode = ((LayerNode) anchorText).node;
                    final Relationship anchorRel = node.createRelationshipTo(anchorTextNode, ANCHORS);

                    final TextRange anchorRange = anchor.getRange();
                    final long rangeStart = anchorRange.getStart();
                    final long rangeEnd = anchorRange.getEnd();

                    anchorRel.setProperty(LayerNode.RANGE_START, rangeStart);
                    anchorRel.setProperty(LayerNode.RANGE_END, rangeEnd);

                    index.add(anchorRel, "id", ValueContext.numeric(node.getId()));
                    index.add(anchorRel, "text", ValueContext.numeric(anchorTextNode.getId()));
                    index.add(anchorRel, "rs", ValueContext.numeric(rangeStart));
                    index.add(anchorRel, "re", ValueContext.numeric(rangeEnd));
                    index.add(anchorRel, "len", ValueContext.numeric(anchorRange.length()));
                    index.add(anchorRel, "ns", ns);
                    index.add(anchorRel, "ln", localName);
                }
            }

            db.getReferenceNode().createRelationshipTo(node, HAS_TEXT);
            return new LayerNode<T>(this, node);
        } finally {
            commit(tx);
        }
    }

    protected RelationshipIndex index() {
        return db.index().forRelationships("anchors");
    }

    @Override
    public Layer<T> add(Name name, Reader text, T data, Anchor... anchors) throws IOException {
        return add(name, text, data, Sets.newHashSet(Arrays.asList(anchors)));
    }

    @Override
    public void delete(Iterable<Layer<T>> layers) {
        final Transaction tx = begin();
        final RelationshipIndex index = index();
        try {
            final Set<Node> nodes = Sets.newHashSet();
            for (LayerNode<?> toDelete : Iterables.filter(layers, LayerNode.class)) {
                nodes.add(toDelete.node);
                for (Relationship anchorRel : TRANSITIVE_ANCHORING.traverse(toDelete.node).relationships()) {
                    nodes.add(anchorRel.getStartNode());
                    index.remove(anchorRel);
                    anchorRel.delete();
                }
            }
            for (Node node : nodes) {
                for (Relationship anchorRel : node.getRelationships(ANCHORS, Direction.OUTGOING)) {
                    index.remove(anchorRel);
                    anchorRel.delete();
                }
                for (Relationship primeRel : node.getRelationships(HAS_TEXT)) {
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
