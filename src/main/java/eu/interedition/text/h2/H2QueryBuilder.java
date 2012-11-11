package eu.interedition.text.h2;

import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import eu.interedition.text.Anchor;
import eu.interedition.text.Layer;
import eu.interedition.text.Name;
import eu.interedition.text.Query;
import eu.interedition.text.QueryResult;
import eu.interedition.text.Text;
import eu.interedition.text.TextRange;
import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class H2QueryBuilder<T> {

    final Joiner WITH_SPACES = Joiner.on(" ");

    public String sql(Query query) {
        final Clause clause = build(query.getRoot());

        final List<String> joins = Lists.newLinkedList();
        final List<String> clauses = Lists.newLinkedList();
        clause.joins(joins, 0);
        clause.clauses(clauses);

        return new StringBuilder()
                .append("SELECT l.id, n.ns, n.ln, n.id, an.ns, an.ln, an.id, al.id, a.range_start, a.range_end")
                .append(" FROM interedition_text_layer l")
                .append(" JOIN interedition_name n ON n.id = l.name_id")
                .append(" LEFT JOIN interedition_text_anchor a ON a.from_id = l.id")
                .append(" LEFT JOIN interedition_layer al ON a.to_id = al.id")
                .append(" LEFT JOIN interedition_name an ON an.id = al.name_id")
                .append(" ").append(WITH_SPACES.join(Iterables.filter(joins, Predicates.not(Predicates.equalTo("")))))
                .append(" WHERE ").append(WITH_SPACES.join(Iterables.filter(clauses, Predicates.not(Predicates.equalTo("")))))
                .append(" ORDER BY l.id, al.id")
                .toString();
    }

    public QueryResult<T> results(final H2TextRepository<T> repository, final Query query) {
        return new QueryResult<T>() {

            Connection connection;

            @Override
            public Iterator<Layer<T>> iterator() {
                try {
                    if (connection == null) {
                        connection = repository.begin();
                    }

                    final Statement queryStmt = connection.createStatement();
                    final ResultSet resultSet = queryStmt.executeQuery(sql(query));
                    return new AbstractIterator<Layer<T>>() {
                        Set<Anchor> anchors = null;
                        LayerRelation<T> layer = null;

                        @Override
                        protected Layer<T> computeNext() {
                            try {
                                Layer<T> result = null;
                                while (resultSet.next()) {
                                    final long layerId = resultSet.getLong(1);
                                    if (layer == null || layer.getId() != layerId) {
                                        if (layer != null || resultSet.isLast()) {
                                            result = layer;
                                        }
                                        layer = new LayerRelation<T>(new NameRelation(resultSet.getString(2), resultSet.getString(3), resultSet.getLong(4)), anchors = Sets.newHashSet(), layerId, repository);
                                    }
                                    final NameRelation targetName = new NameRelation(resultSet.getString(5), resultSet.getString(6), resultSet.getLong(7));
                                    final LayerRelation<T> target = new LayerRelation<T>(targetName, Collections.<Anchor>emptySet(), resultSet.getLong(8), repository);
                                    final TextRange targetRange = new TextRange(resultSet.getLong(9), resultSet.getLong(10));
                                    anchors.add(new Anchor(target, targetRange));

                                    if (result != null) {
                                        break;
                                    }
                                }
                                if (result == null) {
                                    SQL.closeQuietly(resultSet);
                                    SQL.closeQuietly(queryStmt);
                                    repository.commit(connection);
                                }
                                return (result == null ? endOfData() : result);
                            } catch (SQLException e) {
                                throw repository.rollbackAndConvert(connection, e);
                            }
                        }
                    };
                } catch (SQLException e) {
                    throw repository.rollbackAndConvert(connection, e);
                }
            }

            @Override
            public void close() throws Exception {
                SQL.closeQuietly(connection);
                connection = null;
            }

            @Override
            protected void finalize() throws Throwable {
                close();
            }
        };
    }

    protected Clause build(Query input) {
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
        } else if (input instanceof Query.NameQuery) {
            return new NameClause(((Query.NameQuery) input).getName());
        } else if (input instanceof Query.LayerQuery) {
            return new LayerClause(((Query.LayerQuery<?>) input).getLayer());
        } else if (input instanceof Query.TextQuery) {
            return new TextClause(((Query.TextQuery) input).getText());
        }
        throw new IllegalArgumentException(input.toString());
    }

    protected List<Clause> build(List<Query> queries) {
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
        public String toString() {
            final StringBuilder builder = new StringBuilder(relation + ".ln = ").append(name.getLocalName()).append(" AND ");
            final URI ns = name.getNamespace();
            if (ns == null) {
                builder.append(relation + ".ns IS NULL");
            } else {
                builder.append(relation + ".ns = '").append(ns.toString()).append("'"); // FIXME: SQL-escape ns argument
            }
            return builder.toString();
        }
    }

    protected class LayerClause extends Clause {
        final Layer<?> layer;

        LayerClause(Layer<?> layer) {
            this.layer = layer;
        }

        @Override
        public String toString() {
            return (layer instanceof LayerRelation ? ("l.id = " + ((LayerRelation<?>) layer).getId()) : NONE.toString());
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

        protected RangeClause(TextRange range) {
            this.range = range;
        }

        @Override
        public int joins(List<String> parts, int joins) {
            relation += joins;
            parts.add("LEFT JOIN interedition_text_anchor " + relation + " ON " + relation + ".from_id = l.id");
            return ++joins;
        }
    }

    protected class RangeEnclosesClause extends RangeClause {

        RangeEnclosesClause(TextRange range) {
            super(range);
        }

        @Override
        public String toString() {
            return (range.getStart() + "<= " + relation + ".range_start AND " + range.getEnd() + " >= " + relation + ".range_end");
        }
    }

    protected class RangeLengthClause extends RangeClause {

        RangeLengthClause(TextRange range) {
            super(range);
        }

        @Override
        public String toString() {
            return ("(" + relation + ".range_end - " + relation + ".range_start) = " + range.length());
        }
    }

    protected class RangeOverlapClause extends RangeClause {

        RangeOverlapClause(TextRange range) {
            super(range);
        }

        @Override
        public String toString() {
            return ("(" + relation + ".range_start < " + range.getEnd() + ") AND (" + relation + ".range_end > " + range.getStart() + ")");
        }
    }

    protected final Clause ANY = new Clause() {
        @Override
        public String toString() {
            return "1 = 1";
        }
    };

    protected final Clause NONE = new Clause() {
        @Override
        public String toString() {
            return "1 <> 1";
        }
    };
}
