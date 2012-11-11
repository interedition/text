/*
 * #%L
 * Text Repository: Datastore for texts based on Interedition's model.
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
package eu.interedition.text.token;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import eu.interedition.text.AbstractTestResourceTest;
import eu.interedition.text.Anchor;
import eu.interedition.text.Layer;
import eu.interedition.text.Name;
import eu.interedition.text.Query;
import eu.interedition.text.QueryResultTextStream;
import eu.interedition.text.TextConstants;
import eu.interedition.text.TextRange;
import eu.interedition.text.TextRanges;
import eu.interedition.text.simple.KeyValues;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.SortedMap;
import java.util.logging.Level;
import org.junit.Test;

import static eu.interedition.text.Query.and;
import static eu.interedition.text.Query.name;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class TokenizerTest extends AbstractTestResourceTest {

    private static final Name SENTENCE_NAME = new Name(TextConstants.TEI_NS, "s");

    @Test
    public void printTokenization() throws IOException {
        printTokenizedWitness(tokenize(), name(SENTENCE_NAME));
    }

    protected Layer<KeyValues> tokenize() throws IOException {
        final Layer<KeyValues> layer = text("gottsched-cato-tei.xml");
        new QueryResultTextStream<KeyValues>(repository, layer).stream(new Tokenizer<KeyValues>(new TokenizerSettings<KeyValues>() {
            private StringBuffer buf = new StringBuffer();

            @Override
            public boolean startingAnnotationsAreBoundary(Tokenizer<KeyValues> tokenizer, long offset, Iterable<Layer<KeyValues>> annotations) {
                return false;
            }

            @Override
            public boolean endingAnnotationsAreBoundary(Tokenizer<KeyValues> tokenizer, long offset, Iterable<Layer<KeyValues>> annotations) {
                return false;
            }

            @Override
            public boolean isBoundary(Tokenizer<KeyValues> tokenizer, long offset, char c) {
                buf.append(c);
                if (isSentenceBoundary(c, buf.length() - 1)) {
                    return true;
                } else if (isFilling(c)) {
                    for (int i = buf.length() - 2; i >= 0; i--) {
                        if (isSentenceBoundary(buf.charAt(i), i)) {
                            return true;
                        }
                        if (isFilling(buf.charAt(i))) {
                            continue;
                        }
                        break;
                    }
                    return false;
                }
                return false;
            }

            @Override
            public void token(Tokenizer<KeyValues> tokenizer, TextRange range) {
                try {
                    repository.add(SENTENCE_NAME, new StringReader(""), null, new Anchor(layer, range));
                } catch (IOException e) {
                    throw Throwables.propagate(e);
                }
            }

            private boolean isSentenceBoundary(char c, int i) {
                return (c == '!' || c == '?' || c == ':' || c == ';' || (c == '.' && (i == 0 || !Character.isDigit(buf.charAt(i - 1)))));
            }

            private boolean isFilling(char c) {
                return Character.isWhitespace(c) || !Character.isLetterOrDigit(c);
            }
        }));
        return layer;
    }

    protected void printTokenizedWitness(Layer<KeyValues> layer, Query tokenCriterion) throws IOException {
        if (!LOG.isLoggable(Level.FINE)) {
            return;
        }

        long read = 0;

        final SortedMap<TextRange, Boolean> ranges = Maps.newTreeMap();
        for (Layer<KeyValues> token : TextRanges.orderingByTarget(layer).immutableSortedCopy(repository.query(and(Query.text(layer), tokenCriterion)))) {
            for (Anchor anchor : token.getAnchors()) {
                if (anchor.getText().equals(layer)) {
                    final TextRange range = anchor.getRange();
                    if (read < range.getStart()) {
                        ranges.put(new TextRange(read, range.getStart()), false);
                    }
                    ranges.put(range, true);
                    read = range.getEnd();
                }
            }

        }

        final long length = layer.length();
        if (read < length) {
            ranges.put(new TextRange(read, length), false);
        }

        final SortedMap<TextRange, String> texts = layer.read(Sets.newTreeSet(ranges.keySet()));
        StringBuilder tokenized = new StringBuilder();
        for (Map.Entry<TextRange, Boolean> range : ranges.entrySet()) {
            tokenized.append(range.getValue() ? "[" : "");
            tokenized.append(texts.get(range.getKey()));
            tokenized.append(range.getValue() ? "]" : "");
        }
        LOG.fine(tokenized.toString());
    }
}
