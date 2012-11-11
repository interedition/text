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
package eu.interedition.text.xml;

import com.google.common.collect.Lists;
import com.google.common.io.NullOutputStream;
import eu.interedition.text.AbstractTestResourceTest;
import eu.interedition.text.Layer;
import eu.interedition.text.Name;
import eu.interedition.text.Query;
import eu.interedition.text.TextConstants;
import java.io.PrintStream;
import java.util.List;
import java.util.logging.Level;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.ContentHandler;

import static eu.interedition.text.TextConstants.TEI_NS;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class XMLSerializerTest extends AbstractTestResourceTest {

    private static final PrintStream NULL_STREAM = new PrintStream(new NullOutputStream());

    private SAXTransformerFactory transformerFactory;

    @Before
    public void initTransformerFactory() {
        transformerFactory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
    }

    @Test
    public void clixSerialize() throws Exception {
        final Layer testLayer = text("wp-orpheus1-clix.xml");

        repository.delete(repository.query(Query.and(Query.text(testLayer), Query.rangeLength(0))));

        XMLSerializer.serialize(createOutputHandler(), repository, testLayer, new XMLSerializerConfigurationBase() {

            @Override
            public Name getRootName() {
                return new Name(TextConstants.TEI_NS, "text");
            }

            @Override
            public List<Name> getHierarchy() {
                return Lists.newArrayList(
                        new Name(null, "phr"),
                        new Name(null, "s")
                );
            }
        });

        if (LOG.isLoggable(Level.FINE)) {
            System.out.println();
        }
    }

    @Test
    public void teiConversion() throws Exception {
        final Layer testLayer = text("george-algabal-tei.xml");
        repository.delete(repository.query(Query.and(Query.text(testLayer), Query.rangeLength(0))));
        XMLSerializer.serialize(createOutputHandler(), repository, testLayer, new XMLSerializerConfigurationBase() {
            public Name getRootName() {
                return new Name(TEI_NS, "text");
            }

            @Override
            public List<Name> getHierarchy() {
                return Lists.newArrayList(
                        new Name(TEI_NS, "page"),
                        new Name(TEI_NS, "line")
                );
            }

            public Query getQuery() {
                return Query.or(
                        Query.name(new Name(TEI_NS, "page")),
                        Query.name(new Name(TEI_NS, "line"))
                );
            }

        });

        if (LOG.isLoggable(Level.FINE)) {
            System.out.println();
        }
    }

    ContentHandler createOutputHandler() throws Exception {
        final TransformerHandler transformerHandler = transformerFactory.newTransformerHandler();
        transformerHandler.setResult(new StreamResult(LOG.isLoggable(Level.FINE) ? System.out : NULL_STREAM));
        return transformerHandler;
    }
}
