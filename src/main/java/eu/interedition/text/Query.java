/*
 * #%L
 * Text: A text model with range-based markup via standoff annotations.
 * %%
 * Copyright (C) 2010 - 2011 The Interedition Development Group
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package eu.interedition.text;

import com.google.common.collect.Lists;
import java.util.List;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public abstract class Query {

    OperatorQuery parent;

    public Query getRoot() {
        Query root = this;
        while (root.parent != null) {
            root = root.parent;
        }
        return root;
    }

    public static Query any() {
        return new Any();
    }

    public static Query none() {
        return new None();
    }

    public static <T> Query is(Layer<T> layer) {
        return new LayerIdentityQuery<T>(layer.getId());
    }

    public static <T> Query is(long id) {
        return new LayerIdentityQuery<T>(id);
    }

    public static Query name(Name name) {
        return new NameQuery(name);
    }

    public static Query text(Text text) {
        return new TextQuery(text);
    }

    public static Query rangeOverlap(TextRange range) {
        return new RangeQuery.RangeOverlapQuery(range);
    }

    public static Query rangeEncloses(TextRange range) {
        return new RangeQuery.RangeEnclosesQuery(range);
    }

    public static Query rangeLength(int length) {
        return new RangeQuery.RangeLengthQuery(length);
    }

    public static OperatorQuery and(Query... criteria) {
        final OperatorQuery.And andOperator = new OperatorQuery.And();
        for (Query criterion : criteria) {
            andOperator.add(criterion);
        }
        return andOperator;
    }

    public static OperatorQuery or(Query... criteria) {
        final OperatorQuery.Or orOperator = new OperatorQuery.Or();
        for (Query criterion : criteria) {
            orOperator.add(criterion);
        }
        return orOperator;
    }

    public static class Any extends Query {
    }

    public static class None extends Query {
    }

    public abstract static class OperatorQuery extends Query {
        final List<Query> operands = Lists.newLinkedList();

        OperatorQuery() {
        }

        public OperatorQuery add(Query criterion) {
            criterion.parent = this;
            operands.add(criterion);
            return this;
        }

        public List<Query> getOperands() {
            return operands;
        }

        public static class Or extends OperatorQuery {
            Or() {
            }
        }

        public static class And extends OperatorQuery {
            And() {
            }
        }
    }

    public abstract static class RangeQuery extends Query {
        final TextRange range;

        RangeQuery(TextRange range) {
            this.range = range;
        }


        public TextRange getRange() {
            return range;
        }

        public static class RangeOverlapQuery extends RangeQuery {
            RangeOverlapQuery(TextRange range) {
                super(range);
            }
        }

        public static class RangeEnclosesQuery extends RangeQuery {
            RangeEnclosesQuery(TextRange range) {
                super(range);
            }
        }

        public static class RangeLengthQuery extends RangeQuery {

            RangeLengthQuery(long length) {
                super(new TextRange(0, length));
            }
        }
    }

    public static class LayerIdentityQuery<T> extends Query {
        private final long id;

        LayerIdentityQuery(long id) {
            this.id = id;
        }

        public long getId() {
            return id;
        }
    }

    public static class NameQuery extends Query {
        private final Name name;

        NameQuery(Name name) {
            this.name = name;
        }

        public Name getName() {
            return name;
        }
    }

    public static class TextQuery extends Query {
        private final Text text;

        TextQuery(Text text) {
            this.text = text;
        }

        public Text getText() {
            return text;
        }
    }
}
