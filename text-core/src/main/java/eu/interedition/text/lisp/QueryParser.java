package eu.interedition.text.lisp;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import eu.interedition.text.Name;
import eu.interedition.text.Query;
import eu.interedition.text.TextRange;
import eu.interedition.text.TextRepository;
import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class QueryParser<T> {

    private final TextRepository<T> repository;

    public QueryParser(TextRepository<T> repository) {
        this.repository = repository;
    }

    public Query parse(String query) throws LispParserException {
        try {
            final Expression expr = new LispParser(query).expression();
            Preconditions.checkArgument(expr instanceof ExpressionList, expr.toString());
            return translate((ExpressionList) expr);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private Query translate(ExpressionList expr) {
        Preconditions.checkArgument(!expr.isEmpty(), expr.toString());

        final Expression firstExpr = expr.get(0);
        Preconditions.checkArgument(firstExpr instanceof Atom, firstExpr.toString());

        final String functionName = ((Atom) firstExpr).name;

        if ("and".equals(functionName)) {
            return Query.and(translateList(expr.subList(1, expr.size())));
        } else if ("or".equals(functionName)) {
            return Query.or(translateList(expr.subList(1, expr.size())));
        } else if ("text".equals(functionName)) {
            Preconditions.checkArgument(expr.size() > 1, expr.toString());
            return Query.text(repository.findByIdentifier(number(expr.get(1))));
        } else if ("name".equals(functionName)) {
            Preconditions.checkArgument(expr.size() > 1, expr.toString());
            return expr.size() > 2
                    ? Query.name(new Name(URI.create(string(expr.get(2))), string(expr.get(1))))
                    : Query.localName(string(expr.get(1)));
        } else if ("overlaps".equals(functionName)) {
            Preconditions.checkArgument(expr.size() > 2, expr.toString());
            return Query.rangeOverlap(new TextRange(number(expr.get(1)), number(expr.get(2))));
        } else if ("length".equals(functionName)) {
            Preconditions.checkArgument(expr.size() > 1, expr.toString());
            return Query.rangeLength(number(expr.get(1)));
        } else if ("".equals(functionName)) {
            Preconditions.checkArgument(expr.size() > 2, expr.toString());
            return Query.rangeEncloses(new TextRange(number(expr.get(1)), number(expr.get(2))));
        } else if ("any".equals(functionName)) {
            return Query.any();
        } else if ("none".equals(functionName)) {
            return Query.none();
        }
        throw new IllegalArgumentException(expr.toString());
    }

    private Query[] translateList(List<Expression> exprList) {
        Query[] translated = new Query[exprList.size()];
        for (int ec = 0; ec < exprList.size(); ec++) {
            final Expression expr = exprList.get(ec);
            Preconditions.checkArgument(expr instanceof ExpressionList, expr.toString());
            translated[ec] = translate((ExpressionList) expr);
        }
        return translated;
    }

    private long number(Expression expr) {
        Preconditions.checkArgument(expr instanceof NumberAtom, expr.toString());
        return ((NumberAtom) expr).getValue();
    }

    private String string(Expression expr) {
        Preconditions.checkArgument(expr instanceof StringAtom, expr.toString());
        return ((StringAtom) expr).getValue();
    }
}
