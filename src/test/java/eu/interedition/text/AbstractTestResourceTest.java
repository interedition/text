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

import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import eu.interedition.text.simple.KeyValues;
import eu.interedition.text.simple.SimpleTextRepository;
import eu.interedition.text.simple.SimpleXMLTransformerConfiguration;
import eu.interedition.text.xml.XML;
import eu.interedition.text.xml.XMLTransformer;
import eu.interedition.text.xml.XMLTransformerModule;
import eu.interedition.text.xml.module.CLIXAnnotationXMLTransformerModule;
import eu.interedition.text.xml.module.DefaultAnnotationXMLTransformerModule;
import eu.interedition.text.xml.module.LineElementXMLTransformerModule;
import eu.interedition.text.xml.module.NotableCharacterXMLTransformerModule;
import eu.interedition.text.xml.module.TEIAwareAnnotationXMLTransformerModule;
import eu.interedition.text.xml.module.TextXMLTransformerModule;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import org.junit.After;
import org.junit.Before;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import static eu.interedition.text.TextConstants.TEI_NS;

/**
 * Base class for tests working with documents generated from XML test resources.
 *
 * @author <a href="http://gregor.middell.net/" title="Homepage of Gregor Middell">Gregor Middell</a>
 */
public abstract class AbstractTestResourceTest extends AbstractTextTest {
    protected static final XMLInputFactory xmlInputFactory = XML.createXMLInputFactory();
    protected static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();

    protected XMLTransformer<KeyValues> xmlTransformer;

    private Map<URI, Layer<KeyValues>> sources = Maps.newHashMap();
    private Map<URI, Layer<KeyValues>> texts = Maps.newHashMap();

    @Before
    public void createXMLTransformer() {
        xmlTransformer = new XMLTransformer<KeyValues>(configure(createXMLParserConfiguration()));
    }

    @After
    public void removeDocuments() throws IOException {
        repository.delete(texts.values());
        repository.delete(sources.values());

        texts.clear();
        sources.clear();
    }

    protected Layer<KeyValues> text() {
        return text("archimedes-palimpsest-tei.xml");
    }

    protected Layer<KeyValues> source() {
        return source("archimedes-palimpsest-tei.xml");
    }

    protected void unload() throws IOException {
        unload("archimedes-palimpsest-tei.xml");
    }

    protected synchronized Layer<KeyValues> text(String resource) {
        try {
            return text(AbstractTestResourceTest.class.getResource(resource).toURI());
        } catch (URISyntaxException e) {
            throw Throwables.propagate(e);
        }
    }

    protected synchronized Layer<KeyValues> text(URI resource) {
        load(resource);
        return texts.get(resource);
    }

    protected synchronized Layer<KeyValues> source(String resource) {
        try {
            return source(AbstractTestResourceTest.class.getResource(resource).toURI());
        } catch (URISyntaxException e) {
            throw Throwables.propagate(e);
        }
    }

    protected synchronized Layer<KeyValues> source(URI resource) {
        load(resource);
        return sources.get(resource);
    }

    protected List<XMLTransformerModule<KeyValues>> parserModules() {
        return Lists.newArrayList();
    }

    protected SimpleXMLTransformerConfiguration<KeyValues> configure(SimpleXMLTransformerConfiguration<KeyValues> pc) {
        pc.addLineElement(new Name(TEI_NS, "div"));
        pc.addLineElement(new Name(TEI_NS, "head"));
        pc.addLineElement(new Name(TEI_NS, "sp"));
        pc.addLineElement(new Name(TEI_NS, "stage"));
        pc.addLineElement(new Name(TEI_NS, "speaker"));
        pc.addLineElement(new Name(TEI_NS, "lg"));
        pc.addLineElement(new Name(TEI_NS, "l"));
        pc.addLineElement(new Name(TEI_NS, "p"));
        pc.addLineElement(new Name((URI) null, "line"));

        pc.addContainerElement(new Name(TEI_NS, "text"));
        pc.addContainerElement(new Name(TEI_NS, "div"));
        pc.addContainerElement(new Name(TEI_NS, "lg"));
        pc.addContainerElement(new Name(TEI_NS, "subst"));
        pc.addContainerElement(new Name(TEI_NS, "choice"));

        pc.exclude(new Name(TEI_NS, "teiHeader"));
        pc.exclude(new Name(TEI_NS, "front"));
        pc.exclude(new Name(TEI_NS, "fw"));
        pc.exclude(new Name(TEI_NS, "app"));

        pc.include(new Name(TEI_NS, "lem"));

        return pc;
    }

    protected SimpleXMLTransformerConfiguration<KeyValues> createXMLParserConfiguration() {
        SimpleXMLTransformerConfiguration<KeyValues> pc = new SimpleXMLTransformerConfiguration<KeyValues>((SimpleTextRepository<KeyValues>) repository);

        final List<XMLTransformerModule<KeyValues>> modules = pc.getModules();
        modules.add(new LineElementXMLTransformerModule<KeyValues>());
        modules.add(new NotableCharacterXMLTransformerModule<KeyValues>());
        modules.add(new TextXMLTransformerModule<KeyValues>());
        modules.add(new DefaultAnnotationXMLTransformerModule<KeyValues>());
        modules.add(new CLIXAnnotationXMLTransformerModule<KeyValues>());
        modules.add(new TEIAwareAnnotationXMLTransformerModule<KeyValues>());
        modules.addAll(parserModules());

        return pc;
    }

    protected synchronized void load(URI resource) {
        try {
            if (!texts.containsKey(resource)) {
                final Stopwatch stopWatch = new Stopwatch().start();

                InputStream xmlStream = null;
                try {

                    final StringWriter xmlContent = new StringWriter();
                    final Transformer transformer = TRANSFORMER_FACTORY.newTransformer();

                    final XMLReader xmlReader = XMLReaderFactory.createXMLReader();
                    xmlReader.setEntityResolver(new EntityResolver() {
                        @Override
                        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                            return new InputSource(new StringReader(""));
                        }
                    });
                    transformer.transform(new SAXSource(xmlReader, new InputSource(xmlStream = resource.toURL().openStream())), new StreamResult(xmlContent));
                    final Layer<KeyValues> xml = repository.add(new Name(TextConstants.INTEREDITION_NS_URI, "xmlSource"), new StringReader(xmlContent.toString()), null);
                    sources.put(resource, xml);

                    texts.put(resource, xmlTransformer.transform(xml));
                    stopWatch.stop();
                } finally {
                    Closeables.close(xmlStream, false);
                }

                if (LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "{0} parsed in {1}", new Object[]{resource, stopWatch });
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    protected synchronized void unload(String resource) throws IOException {
        try {
            unload(AbstractTestResourceTest.class.getResource(resource).toURI());
        } catch (URISyntaxException e) {
            throw Throwables.propagate(e);
        }
    }

    protected synchronized void unload(URI resource) throws IOException {
        repository.delete(Collections.singleton(texts.remove(resource)));
        repository.delete(Collections.singleton(sources.remove(resource)));
    }
}
