package eu.interedition.text.h2;

import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import eu.interedition.text.Anchor;
import eu.interedition.text.Layer;
import eu.interedition.text.Name;
import eu.interedition.text.Query;
import eu.interedition.text.QueryResult;
import eu.interedition.text.Text;
import eu.interedition.text.TextRange;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class H2Query<T> {

    private final Joiner WITH_SPACES = Joiner.on(" ");

    public Layer<T> byId(H2TextRepository<T> repository, long id) {
        QueryResult<T> results = null;
        try {
            return Iterables.getOnlyElement(results(repository, Query.is(new LayerRelation<T>(null, null, null, id, repository))), null);
        } finally {
            Closeables.closeQuietly(results);
        }
    }

    public QueryResult<T> results(final H2TextRepository<T> repository, final Query query) {

        final Clause clause = build(query.getRoot());

        final List<String> joins = Lists.newLinkedList();
        final List<String> clauses = Lists.newLinkedList();
        final List<String> arguments = Lists.newLinkedList();
        clause.joins(joins, 0);
        clause.clauses(clauses);
        clause.arguments(arguments);

        final String sql = "SELECT l.id, n.ns, n.ln, n.id, l.layer_data, an.ns, an.ln, an.id, al.id, a.id, a.range_start, a.range_end" +
                " FROM interedition_text_layer l" +
                " JOIN interedition_name n ON n.id = l.name_id" +
                " LEFT JOIN interedition_text_anchor a ON a.from_id = l.id" +
                " LEFT JOIN interedition_text_layer al ON a.to_id = al.id" +
                " LEFT JOIN interedition_name an ON an.id = al.name_id" +
                " " + WITH_SPACES.join(Iterables.filter(joins, Predicates.not(Predicates.equalTo("")))) +
                " WHERE " + WITH_SPACES.join(Iterables.filter(clauses, Predicates.not(Predicates.equalTo("")))) +
                " ORDER BY l.id, al.id";

        return new QueryResult<T>() {

            Connection connection;

            @Override
            public Iterator<Layer<T>> iterator() {
                try {
                    if (connection == null) {
                        connection = repository.begin();
                    }

                    return new AbstractIterator<Layer<T>>() {
                        PreparedStatement queryStmt;
                        ResultSet resultSet;

                        Set<Anchor> anchors = null;
                        LayerRelation<T> layer = null;
                        AnchorRelation anchor = null;

                        @Override
                        protected Layer<T> computeNext() {
                            try {
                                if (queryStmt == null) {
                                    queryStmt = connection.prepareStatement(sql);
                                    int ac = 0;
                                    for (String argument : arguments) {
                                        queryStmt.setString(++ac, argument);
                                    }
                                    resultSet = queryStmt.executeQuery();
                                }
                                Layer<T> result = null;
                                while (resultSet != null && resultSet.next()) {
                                    final long layerId = resultSet.getLong(1);
                                    if (layer == null || layer.getId() != layerId) {
                                        T data = null;
                                        final Blob dataBlob = resultSet.getBlob(5);
                                        if (dataBlob != null) {
                                            InputStream dataStream = null;
                                            try {
                                                data = repository.data(dataStream = dataBlob.getBinaryStream());
                                            } finally {
                                                Closeables.close(dataStream, false);
                                            }
                                        }
                                        if (layer != null) {
                                            result = layer;
                                        }
                                        final long layerNameId = resultSet.getLong(4);
                                        NameRelation layerName = repository.cachedName(layerNameId, new NameRelation(resultSet.getString(2), resultSet.getString(3), layerNameId));
                                        layer = new LayerRelation<T>(layerName, anchors = Sets.newHashSet(), data, layerId, repository);
                                        anchor = null;
                                    }
                                    final long anchorId = resultSet.getLong(10);
                                    if (anchorId != 0 && (anchor == null || anchor.getId() != anchorId)) {
                                        final long anchorNameId = resultSet.getLong(8);
                                        final NameRelation targetName = repository.cachedName(anchorNameId, new NameRelation(resultSet.getString(6), resultSet.getString(7), anchorNameId));
                                        final LayerRelation<T> target = new LayerRelation<T>(targetName, Collections.<Anchor>emptySet(), null, resultSet.getLong(9), repository);
                                        final TextRange targetRange = new TextRange(resultSet.getLong(11), resultSet.getLong(12));
                                        anchors.add(anchor = new AnchorRelation(target, targetRange, anchorId));
                                    }

                                    if (result != null) {
                                        break;
                                    }
                                }
                                if (resultSet != null && resultSet.isLast()) {
                                    JdbcUtil.closeQuietly(resultSet);
                                    resultSet = null;
                                    JdbcUtil.closeQuietly(queryStmt);
                                    repository.commit(connection);
                                }
                                if (result == null && layer != null) {
                                    result = layer;
                                    layer = null;
                                }
                                return result == null ? endOfData() : result;
                            } catch (SQLException e) {
                                throw repository.rollbackAndConvert(connection, e);
                            } catch (IOException e) {
                                throw repository.rollbackAndConvert(connection, e);
                            }
                        }
                    };
                } catch (SQLException e) {
                    throw repository.rollbackAndConvert(connection, e);
                }
            }

            @Override
            public void close() throws IOException {
                JdbcUtil.closeQuietly(connection);
                connection = null;
            }

            @Override
            protected void finalize() throws Throwable {
                close();
                super.finalize();
            }
        };
    }

    Clause build(Query input) {
        if (input instanceof Query.Any) {
            return ANY;
        } else if (input instanceof Query.None) {
            return NONE;
        } else if (input instanceof Query.OperatorQuery.And) {
            return new OperatorClause("AND", build(((Query.OperatorQuery) input).getOperands()));
        } else if (input instanceof Query.OperatorQuery.Or) {
            return new OperatorClause("OR", build(((Query.OperatorQuery) input).getOperands()));
        } else if (input instanceof Query.RangeQuery.RangeEnclosesQuery) {
            return new RangeEnclosesClause(((Query.RangeQuery.RangeEnclosesQuery) input).getRange());
        } else if (input instanceof Query.RangeQuery.RangeLengthQuery) {
            return new RangeLengthClause(((Query.RangeQuery.RangeLengthQuery) input).getRange());
        } else if (input instanceof Query.RangeQuery.RangeOverlapQuery) {
            return new RangeOverlapClause(((Query.RangeQuery.RangeOverlapQuery) input).getRange());
        } else if (input instanceof Query.LocalNameQuery) {
            return new LocalNameClause(((Query.LocalNameQuery)input).getLn());
        } else if (input instanceof Query.NameQuery) {
            return new NameClause(((Query.NameQuery) input).getName());
        } else if (input instanceof Query.LayerIdentityQuery) {
            return new LayerClause(((Query.LayerIdentityQuery<?>) input).getId());
        } else if (input instanceof Query.TextQuery) {
            return new TextClause(((Query.TextQuery) input).getText());
        }
        throw new IllegalArgumentException(input.toString());
    }

    List<Clause> build(List<Query> queries) {
        final List<Clause> clauses = Lists.newArrayListWithExpectedSize(queries.size());
        for (Query q : queries) {
            clauses.add(build(q));
        }
        return clauses;
    }

    protected class Clause {

        public int joins(List<String> parts, int joins) {
            return joins;
        }

        public void clauses(List<String> parts) {
            parts.add(toString());
        }

        public void arguments(List<String> arguments) {
        }
    }

    protected class OperatorClause extends Clause {
        final String op;
        final List<Clause> children;

        OperatorClause(String op, List<Clause> children) {
            this.op = op;
            this.children = children;
        }

        @Override
        public int joins(List<String> parts, int joins) {
            for (Clause child : children) {
                joins = child.joins(parts, joins);
            }
            return joins;
        }

        @Override
        public void arguments(List<String> arguments) {
            for (Clause child : children) {
                child.arguments(arguments);
            }
        }

        @Override
        public void clauses(List<String> parts) {
            String op = "";
            for (Clause child : children) {
                parts.add(op);
                parts.add("(");
                child.clauses(parts);
                parts.add(")");
                op = this.op;
            }
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            for (Clause child : children) {
                if (builder.length() > 0) {
                    builder.append(" ").append(op).append(" ");
                }
                builder.append("(").append(child).append(")");
            }
            return builder.toString();
        }
    }

    protected class LocalNameClause extends Clause {

        final String ln;
        String relation = "n";

        LocalNameClause(String ln) {
            this.ln = ln;
        }

        @Override
        public int joins(List<String> parts, int joins) {
            relation += joins;
            parts.add("LEFT JOIN interedition_name " + relation + " ON " + relation + ".id = l.name_id");
            return ++joins;
        }

        @Override
        public void arguments(List<String> arguments) {
            arguments.add(ln);
        }

        @Override
        public String toString() {
            return relation + ".ln = ?";
        }
    }

    protected class NameClause extends Clause {
        final Name name;
        String relation = "n";

        NameClause(Name name) {
            this.name = name;
        }

        @Override
        public int joins(List<String> parts, int joins) {
            relation += joins;
            parts.add("LEFT JOIN interedition_name " + relation + " ON " + relation + ".id = l.name_id");
            return ++joins;
        }

        @Override
        public void arguments(List<String> arguments) {
            arguments.add(name.getLocalName());
            final URI namespace = name.getNamespace();
            if (namespace != null) {
                arguments.add(namespace.toString());
            }
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder(relation + ".ln = ? AND ");
            final URI ns = name.getNamespace();
            if (ns == null) {
                builder.append(relation).append(".ns IS NULL");
            } else {
                builder.append(relation).append(".ns = ?");
            }
            return builder.toString();
        }
    }

    class LayerClause extends Clause {
        private final long id;

        LayerClause(long id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return ("l.id = " + id);
        }
    }

    protected class TextClause extends Clause {
        final Text text;
        String relation = "a";

        TextClause(Text text) {
            this.text = text;
        }

        @Override
        public int joins(List<String> parts, int joins) {
            if (text instanceof LayerRelation) {
                relation += joins;
                parts.add("LEFT JOIN interedition_text_anchor " + relation + " ON " + relation + ".from_id = l.id");
                return ++joins;
            } else {
                return joins;
            }
        }


        @Override
        public String toString() {
            return (text instanceof LayerRelation ? (relation + ".to_id = " + ((LayerRelation<?>) text).getId()) : NONE.toString());
        }
    }

    protected abstract class RangeClause extends Clause {
        final TextRange range;
        String relation = "a";

        RangeClause(TextRange range) {
            this.range = range;
        }

        @Override
        public int joins(List<String> parts, int joins) {
            relation += joins;
            parts.add("LEFT JOIN interedition_text_anchor " + relation + " ON " + relation + ".from_id = l.id");
            return ++joins;
        }
    }

    class RangeEnclosesClause extends RangeClause {

        RangeEnclosesClause(TextRange range) {
            super(range);
        }

        @Override
        public String toString() {
            return (range.getStart() + " <= " + relation + ".range_start AND " + range.getEnd() + " >= " + relation + ".range_end");
        }
    }

    class RangeLengthClause extends RangeClause {

        RangeLengthClause(TextRange range) {
            super(range);
        }

        @Override
        public String toString() {
            return ("(" + relation + ".range_end - " + relation + ".range_start) = " + range.length());
        }
    }

    class RangeOverlapClause extends RangeClause {

        RangeOverlapClause(TextRange range) {
            super(range);
        }

        @Override
        public String toString() {
            return ("(" + relation + ".range_start < " + range.getEnd() + ") AND (" + relation + ".range_end > " + range.getStart() + ")");
        }
    }

    private final Clause ANY = new Clause() {
        @Override
        public String toString() {
            return "1 = 1";
        }
    };

    private final Clause NONE = new Clause() {
        @Override
        public String toString() {
            return "1 <> 1";
        }
    };
}
