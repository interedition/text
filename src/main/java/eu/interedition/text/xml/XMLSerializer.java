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

import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import eu.interedition.text.Layer;
import eu.interedition.text.Name;
import eu.interedition.text.QueryResultTextStream;
import eu.interedition.text.Text;
import eu.interedition.text.TextConstants;
import eu.interedition.text.TextRange;
import eu.interedition.text.TextRepository;
import eu.interedition.text.TextStream;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamException;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class XMLSerializer<T> extends TextStream.ExceptionPropagatingListenerAdapter<T> {
    private final ContentHandler xml;
    private final XMLSerializerConfiguration<T> config;

    private final Map<URI, String> namespaceMappings = Maps.newHashMap();
    private final Stack<Set<URI>> namespaceMappingStack = new Stack<Set<URI>>();
    private final Map<String, Integer> clixIdIncrements = Maps.newHashMap();
    private final Map<Layer, String> clixIds = Maps.newHashMap();
    private final Ordering<Layer<T>> layerOrdering;

    private boolean rootWritten = false;

    private XMLSerializer(ContentHandler xml, XMLSerializerConfiguration<T> config) {
        this.xml = xml;
        this.config = config;
        this.namespaceMappings.put(URI.create(XMLConstants.XML_NS_URI), XMLConstants.XML_NS_PREFIX);
        this.namespaceMappings.put(URI.create(XMLConstants.XMLNS_ATTRIBUTE_NS_URI), XMLConstants.XMLNS_ATTRIBUTE);
        this.layerOrdering = Ordering.from(new HierarchyAwareAnnotationComparator<T>())
                .compound(Ordering.from(new XMLNodePathComparator<T>(config)))
                .compound(Ordering.arbitrary());
    }

    public static <T> void serialize(final ContentHandler xml, TextRepository<T> repository, Text text, final XMLSerializerConfiguration<T> config) throws XMLStreamException, IOException {
        try {
            new QueryResultTextStream<T>(repository, text, config.getQuery()).stream(new XMLSerializer<T>(xml, config));
        } catch (Throwable t) {
            Throwables.propagateIfInstanceOf(t, IOException.class);
            Throwables.propagateIfInstanceOf(Throwables.getRootCause(t), XMLStreamException.class);
            throw Throwables.propagate(t);
        }
    }

    @Override
    protected void doStart(long contentLength) throws Exception {
        xml.startDocument();

        final Name rootName = config.getRootName();
        if (rootName != null) {
            startElement(rootName, Collections.<Name, String>emptyMap());
        }
    }

    @Override
    protected void doStart(long offset, Iterable<Layer<T>> annotations) throws Exception {
        for (Layer<T> a : layerOrdering.immutableSortedCopy(annotations)) {
            final Name name = a.getName();
            Map<Name, String> attributes = config.extractAttributes(a);
            if (!rootWritten || config.getHierarchy().contains(name)) {
                startElement(name, attributes);
            } else {
                final String localName = name.getLocalName();

                Integer id = clixIdIncrements.get(localName);
                id = (id == null ? 0 : id + 1);
                final String clixId = "clix:" + localName + "-" + id;

                attributes = Maps.newHashMap(attributes);
                attributes.put(TextConstants.CLIX_START_ATTR_NAME, clixId);
                emptyElement(name, attributes);

                clixIdIncrements.put(localName, id);
                clixIds.put(a, clixId);
            }
        }
    }

    @Override
    protected void doEnd(long offset, Iterable<Layer<T>> annotations) throws Exception {
        for (Layer a : layerOrdering.reverse().immutableSortedCopy(annotations)) {
            final String clixId = clixIds.get(a);
            if (clixId == null) {
                endElement(a.getName());
            } else {
                final Map<Name, String> attributes = Maps.newHashMap();
                attributes.put(TextConstants.CLIX_END_ATTR_NAME, clixId);
                emptyElement(a.getName(), attributes);

                clixIds.remove(a);
            }

        }
    }

    @Override
    protected void doText(TextRange r, String text) throws Exception {
        final char[] chars = text.toCharArray();
        xml.characters(chars, 0, chars.length);
    }

    @Override
    protected void doEnd() throws Exception {
        final Name rootName = config.getRootName();
        if (rootName != null) {
            endElement(rootName);
        }
        xml.endDocument();
    }

    private void emptyElement(Name name, Map<Name, String> attributes) throws SAXException {
        startElement(name, attributes);
        endElement(name);
    }

    private void startElement(Name name, Map<Name, String> attributes) throws SAXException {
        namespaceMappingStack.push(new HashSet<URI>());

        final Map<Name, String> nsAttributes = Maps.newHashMap();
        if (!rootWritten) {
            for (Map.Entry<String, URI> mapping : config.getNamespaceMappings().entrySet()) {
                mapNamespace(mapping.getValue(), mapping.getKey(), nsAttributes);
            }
            mapNamespace(TextConstants.CLIX_NS, TextConstants.CLIX_NS_PREFIX, nsAttributes);
            rootWritten = true;
        }
        for (Name n : Iterables.concat(attributes.keySet(), Collections.singleton(name))) {
            final URI ns = n.getNamespace();
            if (ns == null || namespaceMappings.containsKey(ns)) {
                continue;
            }
            int count = 0;
            String newPrefix = "ns" + count;
            while (true) {
                if (!namespaceMappings.containsKey(newPrefix)) {
                    break;
                }
                newPrefix = "ns" + (++count);
            }
            mapNamespace(ns, newPrefix, nsAttributes);
        }

        final Map<Name, String> mergedAttributes = Maps.newLinkedHashMap();
        mergedAttributes.putAll(nsAttributes);
        mergedAttributes.putAll(attributes);
        xml.startElement(toNamespace(name.getNamespace()), name.getLocalName(), toQNameStr(name), toAttributes(mergedAttributes));
    }

    private void mapNamespace(URI namespace, String prefix, Map<Name, String> nsAttributes) throws SAXException {
        final String uri = namespace.toString();
        namespaceMappings.put(namespace, prefix);
        namespaceMappingStack.peek().add(namespace);
        if (prefix.length() == 0) {
            nsAttributes.put(new Name((URI) null, XMLConstants.XMLNS_ATTRIBUTE), uri);
        } else {
            nsAttributes.put(new Name(TextConstants.XMLNS_ATTRIBUTE_NS_URI, prefix), uri);
            xml.startPrefixMapping(prefix, uri);
        }
    }

    private void endElement(Name name) throws SAXException {
        xml.endElement(toNamespace(name.getNamespace()), name.getLocalName(), toQNameStr(name));

        for (URI namespace : namespaceMappingStack.pop()) {
            xml.endPrefixMapping(namespaceMappings.remove(namespace));
        }
    }

    private String toNamespace(URI uri) {
        return (uri == null ? "" : uri.toString());
    }

    private String toQNameStr(Name name) {
        final URI ns = name.getNamespace();
        final String localName = name.getLocalName();

        if (ns == null) {
            return localName;
        } else {
            final String prefix = namespaceMappings.get(ns);
            return (prefix.length() == 0 ? localName : prefix + ":" + localName);
        }
    }

    private Name toQName(String str) {
        final int colon = str.indexOf(':');
        return (colon >= 0 ? toQName(str.substring(0, colon), str.substring(colon + 1)) : toQName(null, str));
    }

    private Name toQName(String uri, String localName) {
        return new Name(URI.create(uri), localName);
    }

    private Attributes toAttributes(final Map<Name, String> attributes) {
        return new Attributes() {
            final List<Name> names = Lists.newArrayList(attributes.keySet());

            public int getLength() {
                return names.size();
            }

            public String getURI(int index) {
                return toNamespace(names.get(index).getNamespace());
            }

            public String getLocalName(int index) {
                return names.get(index).getLocalName();
            }

            public String getQName(int index) {
                return toQNameStr(names.get(index));
            }

            public String getType(int index) {
                return (index >= 0 && index < names.size() ? "CDATA" : null);
            }

            public String getValue(int index) {
                return attributes.get(names.get(index));
            }

            public int getIndex(String uri, String localName) {
                return names.indexOf(toQName(uri, localName));
            }

            public int getIndex(String qName) {
                return names.indexOf(toQName(qName));
            }

            public String getType(String uri, String localName) {
                return names.indexOf(toQName(uri, localName)) >= 0 ? "CDATA" : null;
            }

            public String getType(String qName) {
                return names.indexOf(toQName(qName)) >= 0 ? "CDATA" : null;
            }

            public String getValue(String uri, String localName) {
                return attributes.get(toQName(uri, localName));
            }

            public String getValue(String qName) {
                return attributes.get(toQName(qName));
            }
        };
    }

    private class HierarchyAwareAnnotationComparator<T> implements Comparator<Layer<T>> {
        private final Ordering<Name> hierarchyOrdering;

        private HierarchyAwareAnnotationComparator() {
            this.hierarchyOrdering = Ordering.explicit(Objects.<List<Name>>firstNonNull(config.getHierarchy(), Collections.<Name>emptyList()));
        }

        public int compare(Layer<T> o1, Layer<T> o2) {
            final Name o1Name = o1.getName();
            final Name o2Name = o2.getName();
            if (config.getHierarchy().contains(o1Name) && config.getHierarchy().contains(o2Name)) {
                return hierarchyOrdering.compare(o1Name, o2Name);
            }
            return 0;
        }
    }

    private class XMLNodePathComparator<T> implements Comparator<Layer<T>> {

        private final XMLSerializerConfiguration<T> config;

        private XMLNodePathComparator(XMLSerializerConfiguration<T> config) {
            this.config = config;
        }

        public int compare(Layer<T> o1, Layer<T> o2) {
            final XMLNodePath o1Path = config.extractXMLNodePath(o1);
            final XMLNodePath o2Path = config.extractXMLNodePath(o2);
            if (o1Path != null && o2Path != null) {
                return o1Path.compareTo(o2Path);
            }
            return 0;
        }
    }
}
