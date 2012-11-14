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
package eu.interedition.text.rdbms;

import com.google.common.collect.Iterables;
import eu.interedition.text.AbstractTestResourceTest;
import eu.interedition.text.Layer;
import eu.interedition.text.Query;
import eu.interedition.text.QueryResult;
import eu.interedition.text.TextRange;
import eu.interedition.text.simple.KeyValues;
import java.io.IOException;
import org.junit.Test;

import static com.google.common.collect.Iterables.size;
import static eu.interedition.text.Query.and;
import static eu.interedition.text.Query.rangeEncloses;
import static junit.framework.Assert.assertTrue;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class AnnotationTest extends AbstractTestResourceTest {

    @Test
    public void deleteAll() throws IOException {
        final Layer existing = text();
        try {
            final QueryResult<KeyValues> layers = repository.query(and(Query.text(existing), rangeEncloses(new TextRange(0, existing.length()))));
            repository.delete(layers);
            final Iterable<Layer<KeyValues>> remaining = repository.query(Query.text(existing));
            assertTrue(Integer.toString(size(remaining)) + " in " + existing, Iterables.isEmpty(remaining));
        } finally {
            unload();
        }
    }
}
