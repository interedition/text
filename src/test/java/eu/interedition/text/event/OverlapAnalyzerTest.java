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
package eu.interedition.text.event;

import com.google.common.collect.Iterables;
import eu.interedition.text.AbstractTestResourceTest;
import eu.interedition.text.Anchor;
import eu.interedition.text.Layer;
import eu.interedition.text.Name;
import eu.interedition.text.QueryResultTextStream;
import eu.interedition.text.TextRange;
import eu.interedition.text.simple.KeyValues;
import eu.interedition.text.util.OverlapAnalyzer;
import java.io.IOException;
import java.io.StringReader;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class OverlapAnalyzerTest extends AbstractTestResourceTest {

    @Test
    public void analyzeNonOverlap() throws IOException {
        final OverlapAnalyzer<KeyValues> analyzer = analyze(text());
        Assert.assertEquals(0, analyzer.getOverlapping().size());
        Assert.assertEquals(0, analyzer.getSelfOverlapping().size());
    }

    @Test
    public void analyzeSelfOverlap() throws IOException {
        final Name overlap = new Name(TEST_NS, "overlap");
        repository.add(overlap, new StringReader(""), null, new Anchor(layer, new TextRange(0, TEST_TEXT.length() - 1)));
        repository.add(overlap, new StringReader(""), null, new Anchor(layer, new TextRange(1, TEST_TEXT.length())));

        final OverlapAnalyzer<KeyValues> analyzer = analyze(layer);
        Assert.assertEquals(0, analyzer.getOverlapping().size());
        Assert.assertEquals(1, analyzer.getSelfOverlapping().size());
        Assert.assertEquals(overlap, Iterables.getOnlyElement(analyzer.getSelfOverlapping()));
    }

    OverlapAnalyzer<KeyValues> analyze(Layer<KeyValues> layer) throws IOException {
        final OverlapAnalyzer<KeyValues> analyzer = new OverlapAnalyzer<KeyValues>();
        new QueryResultTextStream<KeyValues>(repository, layer).stream(analyzer);
        return analyzer;
    }
}
