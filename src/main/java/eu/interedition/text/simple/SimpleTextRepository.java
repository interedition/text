package eu.interedition.text.simple;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
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
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class SimpleTextRepository<T> implements TextRepository<T> {

    private Set<Layer<T>> contents = Sets.newHashSet();
    private SetMultimap<Text, Layer<T>> targets = HashMultimap.create();

    public Layer<T> add(Name name, Reader text, T data, Set<Anchor> anchors) throws IOException {
        final SimpleLayer<T> added = new SimpleLayer<T>(name, CharStreams.toString(text), data, anchors);
        for (Anchor anchor : anchors) {
            this.targets.put(anchor.getText(), added);
        }
        contents.add(added);
        return added;
    }

    public Layer<T> add(Name name, Reader text, T data, Anchor... anchors) throws IOException {
        return add(name, text, data, Sets.newHashSet(Arrays.asList(anchors)));
    }

    @Override
    public QueryResult<T> query(Query query) {
        return new SimpleQueryResult<T>(Iterables.filter(contents, TO_PREDICATE.apply(query.getRoot())));
    }

    @Override
    public void delete(Iterable<Layer<T>> layers) {
        for (Layer<T> layer : Lists.newLinkedList(layers)) {
            if (contents.remove(layer)) {
                for (Layer<T> dependent : targets.removeAll(layer)) {
                    delete(Collections.singleton(dependent));
                }
            }
        }
    }

    static class SimpleQueryResult<T> implements QueryResult<T> {
        final Iterable<Layer<T>> result;

        SimpleQueryResult(Iterable<Layer<T>> result) {
            this.result = result;
        }

        @Override
        public Iterator<Layer<T>> iterator() {
            return result.iterator();
        }

        @Override
        public void close() throws Exception {
        }
    }
    protected final Function<Query, Predicate<Layer<T>>> TO_PREDICATE = new Function<Query, Predicate<Layer<T>>>() {
        @SuppressWarnings("unchecked")
        @Override
        public Predicate<Layer<T>> apply(@Nullable Query input) {
            if (input instanceof Query.Any) {
                return Predicates.alwaysTrue();
            } else if (input instanceof Query.None) {
                return Predicates.alwaysFalse();
            } else if (input instanceof Query.OperatorQuery.And) {
                return Predicates.and(Iterables.transform(((Query.OperatorQuery) input).getOperands(), this));
            } else if (input instanceof Query.OperatorQuery.Or) {
                return Predicates.or(Iterables.transform(((Query.OperatorQuery) input).getOperands(), this));
            } else if (input instanceof Query.RangeQuery.RangeEnclosesQuery) {
                return new AnyAnchorPredicate<T>(new RangeEnclosesPredicate(((Query.RangeQuery.RangeEnclosesQuery) input).getRange()));
            } else if (input instanceof Query.RangeQuery.RangeLengthQuery) {
                return new AnyAnchorPredicate<T>(new RangeLengthPredicate(((Query.RangeQuery.RangeLengthQuery) input).getRange().length()));
            } else if (input instanceof Query.RangeQuery.RangeOverlapQuery) {
                return new AnyAnchorPredicate<T>(new RangeOverlapPredicate(((Query.RangeQuery.RangeOverlapQuery) input).getRange()));
            } else if (input instanceof Query.NameQuery) {
                return new NamePredicate<T>(((Query.NameQuery) input).getName());
            } else if (input instanceof Query.LayerQuery) {
                return Predicates.equalTo(((Query.LayerQuery<T>) input).getLayer());
            } else if (input instanceof Query.TextQuery) {
                return new AnyAnchorPredicate<T>(new TextPredicate(((Query.TextQuery) input).getText()));
            }
            throw new IllegalArgumentException(input.toString());
        }
    };

    void write(Layer<T> target, Reader text) throws IOException {
        ((SimpleLayer<T>)target).text = CharStreams.toString(text);
    }

    public static class AnyAnchorPredicate<T> implements Predicate<Layer<T>> {

        private Predicate<Anchor> anchorPredicate;

        public AnyAnchorPredicate(Predicate<Anchor> anchorPredicate) {
            this.anchorPredicate = anchorPredicate;
        }

        @Override
        public boolean apply(@Nullable Layer<T> input) {
            return Iterables.any(input.getAnchors(), anchorPredicate);
        }
    }

    public static class NamePredicate<T> implements Predicate<Layer<T>> {

        private final Name name;

        public NamePredicate(Name name) {
            this.name = name;
        }

        @Override
        public boolean apply(@Nullable Layer input) {
            return input.getName().equals(name);
        }
    }

    public static class TextPredicate implements Predicate<Anchor> {

        private final Text target;

        public TextPredicate(Text target) {
            this.target = target;
        }

        @Override
        public boolean apply(@Nullable Anchor input) {
            return input.getText().equals(target);
        }
    }

    public static class RangeEnclosesPredicate implements Predicate<Anchor> {

        private final TextRange range;

        public RangeEnclosesPredicate(TextRange range) {
            this.range = range;
        }

        @Override
        public boolean apply(@Nullable Anchor input) {
            return range.encloses(input.getRange());
        }
    }

    public static class RangeLengthPredicate implements Predicate<Anchor> {

        private final long length;

        public RangeLengthPredicate(long length) {
            this.length = length;
        }

        @Override
        public boolean apply(@Nullable Anchor input) {
            return (input.getRange().length() == length);
        }
    }

    public static class RangeOverlapPredicate implements Predicate<Anchor> {

        private final TextRange range;

        public RangeOverlapPredicate(TextRange range) {
            this.range = range;
        }

        @Override
        public boolean apply(@Nullable Anchor input) {
            return range.hasOverlapWith(input.getRange());
        }
    }
}
