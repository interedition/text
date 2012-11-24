package eu.interedition.text.neo4j;

import com.google.common.collect.Lists;
import eu.interedition.text.Name;
import eu.interedition.text.Query;
import eu.interedition.text.Text;
import eu.interedition.text.TextRange;
import java.net.URI;
import java.util.List;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.util.NumericUtils;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class Neo4jIndexQuery {

    org.apache.lucene.search.Query build(Query input) {
        if (input instanceof Query.Any) {
            return new MatchAllDocsQuery();
        } else if (input instanceof Query.None) {
            return NumericRangeQuery.newLongRange("id", 0L, 0L, true, true);
        } else if (input instanceof Query.OperatorQuery.And) {
            final BooleanQuery andQuery = new BooleanQuery();
            for (org.apache.lucene.search.Query query : build(((Query.OperatorQuery) input).getOperands())) {
                andQuery.add(query, BooleanClause.Occur.MUST);
            }
            return andQuery;
        } else if (input instanceof Query.OperatorQuery.Or) {
            final BooleanQuery orQuery = new BooleanQuery();
            for (org.apache.lucene.search.Query query : build(((Query.OperatorQuery) input).getOperands())) {
                orQuery.add(query, BooleanClause.Occur.SHOULD);
            }
            return orQuery;
        } else if (input instanceof Query.RangeQuery.RangeEnclosesQuery) {
            final TextRange range = ((Query.RangeQuery.RangeEnclosesQuery) input).getRange();
            final BooleanQuery enclosesQuery = new BooleanQuery();
            enclosesQuery.add(NumericRangeQuery.newLongRange("from", range.getStart(), null, true, false), BooleanClause.Occur.MUST);
            enclosesQuery.add(NumericRangeQuery.newLongRange("to", null, range.getEnd(), false, true), BooleanClause.Occur.MUST);
            return enclosesQuery;
        } else if (input instanceof Query.RangeQuery.RangeLengthQuery) {
            final TextRange range = ((Query.RangeQuery.RangeLengthQuery) input).getRange();
            return new TermQuery(new Term("len", NumericUtils.longToPrefixCoded(range.length())));
        } else if (input instanceof Query.RangeQuery.RangeOverlapQuery) {
            final TextRange range = ((Query.RangeQuery.RangeOverlapQuery) input).getRange();
            final BooleanQuery overlapsQuery = new BooleanQuery();
            overlapsQuery.add(NumericRangeQuery.newLongRange("from", null, range.getEnd(), false, false), BooleanClause.Occur.MUST);
            overlapsQuery.add(NumericRangeQuery.newLongRange("to", range.getStart(), null, false, false), BooleanClause.Occur.MUST);
            return overlapsQuery;
        } else if (input instanceof Query.NameQuery) {
            final Name name = ((Query.NameQuery) input).getName();
            final URI ns = name.getNamespace();
            final BooleanQuery nameQuery = new BooleanQuery();
            nameQuery.add(new TermQuery(new Term("ln", name.getLocalName())), BooleanClause.Occur.MUST);
            nameQuery.add(new TermQuery(new Term("ns", ns == null ? "" : ns.toString())), BooleanClause.Occur.MUST);
            return nameQuery;
        } else if (input instanceof Query.LayerIdentityQuery) {
            final long id = ((Query.LayerIdentityQuery<?>) input).getId();
            final BooleanQuery idQuery = new BooleanQuery();
            idQuery.add(new TermQuery(new Term("id", NumericUtils.longToPrefixCoded(id))), BooleanClause.Occur.MUST);
            idQuery.add(new WildcardQuery(new Term("ln", "*")), BooleanClause.Occur.MUST);
            return idQuery;
        } else if (input instanceof Query.TextQuery) {
            final Text text = ((Query.TextQuery) input).getText();
            long textId = (text instanceof LayerNode ? ((LayerNode<?>)text).getId() : 0);
            return new TermQuery(new Term("text", NumericUtils.longToPrefixCoded(textId)));
        }
        throw new IllegalArgumentException(input.toString());
    }

    List<org.apache.lucene.search.Query> build(List<Query> queries) {
        final List<org.apache.lucene.search.Query> clauses = Lists.newArrayListWithExpectedSize(queries.size());
        for (Query q : queries) {
            clauses.add(build(q));
        }
        return clauses;
    }
}
