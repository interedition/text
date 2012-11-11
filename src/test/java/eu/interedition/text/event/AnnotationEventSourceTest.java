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
import eu.interedition.text.Layer;
import eu.interedition.text.Name;
import eu.interedition.text.QueryResultTextStream;
import eu.interedition.text.TextRange;
import eu.interedition.text.TextStream;
import eu.interedition.text.simple.KeyValues;
import java.io.IOException;
import org.junit.Test;

import static eu.interedition.text.Query.name;
import static eu.interedition.text.Query.or;
import static eu.interedition.text.TextConstants.TEI_NS;

public class AnnotationEventSourceTest extends AbstractTestResourceTest {

    @Test
    public void generateEvents() throws IOException {
        new QueryResultTextStream<KeyValues>(repository, text("george-algabal-tei.xml"), or(
                name(new Name(TEI_NS, "div")),
                name(new Name(TEI_NS, "lg")),
                name(new Name(TEI_NS, "l")),
                name(new Name(TEI_NS, "p"))
        )).stream(DEBUG_LISTENER);
    }

    private final TextStream.Listener<KeyValues> DEBUG_LISTENER = new TextStream.Listener<KeyValues>() {

        public void start(long contentLength) {
            LOG.fine("START TEXT: (" + contentLength + " character(s))");
        }

        public void start(long offset, Iterable<Layer<KeyValues>> annotations) {
            LOG.fine("START: [" + offset + "] " + Iterables.toString(annotations));
        }

        public void empty(long offset, Iterable<Layer<KeyValues>> annotations) {
            LOG.fine("EMPTY: [" + offset + "] " + Iterables.toString(annotations));
        }

        public void end(long offset, Iterable<Layer<KeyValues>> annotations) {
            LOG.fine("END: [" + offset + "] " + Iterables.toString(annotations));
        }

        public void text(TextRange r, String text) {
            LOG.fine("TEXT: " + r + " == \"" + escapeNewlines(text) + "\"");
        }

        public void end() {
        }
    };
}
