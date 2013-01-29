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
import eu.interedition.text.simple.SimpleLayer;
import eu.interedition.text.xml.XML;
import eu.interedition.text.xml.XMLNodePath;
import eu.interedition.text.xml.XMLSerializerConfiguration;
import eu.interedition.text.xml.XMLTransformer;
import eu.interedition.text.xml.XMLTransformerConfigurationBase;
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
import java.util.Set;
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
import static eu.interedition.text.TextConstants.XML_SOURCE_NAME;

/**
 * Base class for tests working with documents generated from XML test resources.
 *
 * @author <a href="http://gregor.middell.net/" title="Homepage of Gregor Middell">Gregor Middell</a>
 */
public abstract class AbstractTestResourceTest extends AbstractTextTest {
    protected static final XMLInputFactory xmlInputFactory = XML.createXMLInputFactory();
    private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();

    private XMLTransformer<KeyValues> xmlTransformer;

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

    XMLTransformerConfigurationBase<KeyValues> configure(XMLTransformerConfigurationBase<KeyValues> pc) {
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

    XMLTransformerConfigurationBase<KeyValues> createXMLParserConfiguration() {
        XMLTransformerConfigurationBase<KeyValues> pc = new XMLTransformerConfiguration(repository).withBatchSize(1024);

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

    synchronized void load(URI resource) {
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
                    final Layer<KeyValues> xml = repository.add(XML_SOURCE_NAME, new StringReader(xmlContent.toString()), null, Collections.<Anchor<KeyValues>>emptySet());
                    sources.put(resource, xml);

                    texts.put(resource, xmlTransformer.transform(xml));
                    stopWatch.stop();
                } finally {
                    Closeables.close(xmlStream, false);
                }

                if (LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "{0} parsed in {1}", new Object[]{resource, stopWatch});
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    synchronized void unload(String resource) throws IOException {
        try {
            unload(AbstractTestResourceTest.class.getResource(resource).toURI());
        } catch (URISyntaxException e) {
            throw Throwables.propagate(e);
        }
    }

    synchronized void unload(URI resource) throws IOException {
        repository.delete(Collections.singleton(texts.remove(resource)));
        repository.delete(Collections.singleton(sources.remove(resource)));
    }

    protected class XMLTransformerConfiguration extends XMLTransformerConfigurationBase<KeyValues> {

        protected XMLTransformerConfiguration(TextRepository<KeyValues> repository) {
            super(repository);
        }

        @Override
        protected Layer<KeyValues> translate(Name name, Map<Name, String> attributes, Set<Anchor<KeyValues>> anchors) {
            final KeyValues kv = new KeyValues();
            for (Map.Entry<Name, String> attr : attributes.entrySet()) {
                kv.put(attr.getKey().toString(), attr.getValue());
            }
            return new SimpleLayer<KeyValues>(name, "", kv, anchors);
        }
    }

    protected abstract class XMLSerializerConfigurationBase implements XMLSerializerConfiguration<KeyValues> {
        public Name getRootName() {
            return null;
        }

        public Map<String, URI> getNamespaceMappings() {
            final Map<String, URI> nsMap = Maps.newHashMap();
            nsMap.put("", TEI_NS);
            nsMap.put("ie", TextConstants.INTEREDITION_NS_URI);
            nsMap.put(TextConstants.CLIX_NS_PREFIX, TextConstants.CLIX_NS);
            return nsMap;
        }

        public Query getQuery() {
            return Query.any();
        }

        @Override
        public Map<Name, String> extractAttributes(Layer<KeyValues> layer) {
            final Map<Name, String> attributes = Maps.newHashMap();
            for (Map.Entry<String, Object> kv : layer.data().entrySet()) {
                attributes.put(Name.fromString(kv.getKey()), kv.getValue().toString());
            }
            return attributes;
        }

        @Override
        public XMLNodePath extractXMLNodePath(Layer<KeyValues> layer) {
            final Object xmlNode = layer.data().get(TextConstants.XML_NODE_ATTR_NAME.toString());
            return (xmlNode == null || xmlNode instanceof XMLNodePath) ? (XMLNodePath) xmlNode : XMLNodePath.fromString(xmlNode.toString());
        }
    }
}
