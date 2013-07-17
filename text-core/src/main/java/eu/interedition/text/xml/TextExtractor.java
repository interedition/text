/*
 * Copyright (c) 2013 The Interedition Development Group.
 *
 * This file is part of CollateX.
 *
 * CollateX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CollateX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CollateX.  If not, see <http://www.gnu.org/licenses/>.
 */

package eu.interedition.text.xml;

import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;

import javax.xml.namespace.QName;
import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class TextExtractor implements StreamFilter {

    private WhitespaceCompressor whitespaceCompressor;
    private OffsetMapper offsetMapper;
    private NodePath nodePath;
    private Map<String, String> namespaceMapping;
    private Iterable<TextExtractorComponent> components;
    private int offset;

    public TextExtractor withWhitespaceCompression() {
        return withWhitespaceCompression(new ContainerElementContext());
    }

    public TextExtractor withWhitespaceCompression(ContainerElementContext containerElementContext) {
        this.whitespaceCompressor = new WhitespaceCompressor(containerElementContext);
        return this;
    }

    public TextExtractor withOffsetMapping() {
        this.offsetMapper = new OffsetMapper();
        return this;
    }

    public TextExtractor withNodePath() {
        this.nodePath = new NodePath();
        return this;
    }

    public TextExtractor withNamespaceMapping(Map<String, String> namespaceMapping) {
        this.namespaceMapping = namespaceMapping;
        return this;
    }

    public XMLStreamReader execute(XMLInputFactory inputFactory, XMLStreamReader source, StreamFilter... filters) throws XMLStreamException {
        return execute(inputFactory, source, Arrays.asList(filters));
    }

    public XMLStreamReader execute(XMLInputFactory inputFactory, XMLStreamReader source, Iterable<StreamFilter> filters) throws XMLStreamException {
        final List<StreamFilter> chain = Lists.newLinkedList();

        if (nodePath != null) {
            chain.add(nodePath.reset());
        }

        offset = 0;
        components = Iterables.filter(filters, TextExtractorComponent.class);

        for (TextExtractorComponent component : components) {
            component.extractor = this;
            if (component.preTextExtraction()) {
                chain.add(component);
            }
        }

        if (offsetMapper != null) {
            chain.add(offsetMapper.reset(this));
        }
        if (whitespaceCompressor != null) {
            chain.add(whitespaceCompressor.reset());
        }
        chain.add(this);

        for (TextExtractorComponent component : components) {
            if (!component.preTextExtraction()) {
                chain.add(component);
            }
        }
        for (StreamFilter filter : Iterables.filter(filters, Predicates.not(Predicates.instanceOf(TextExtractorComponent.class)))) {
            chain.add(filter);
        }

        XMLStreamReader reader = source;
        for (StreamFilter filter : chain) {
            reader = inputFactory.createFilteredReader(reader, filter);
        }
        while (reader.hasNext()) {
            reader.next();
        }

        for (AnnotationWriter annotationWriter : Iterables.filter(components, AnnotationWriter.class)) {
            annotationWriter.flush();
        }

        return reader;
    }

    @Override
    public boolean accept(XMLStreamReader reader) {
        if (reader.isCharacters()) {
            write(reader.getText(), true);
        }
        return true;
    }

    public int offset() {
        return offset;
    }

    public NodePath nodePath() {
        return nodePath;
    }

    public int insert(String text) {
        return write(text, false);
    }

    public String name(QName name) {
        if (namespaceMapping == null) {
            return name.toString();
        }
        final String prefix = Strings.nullToEmpty(namespaceMapping.get(Strings.nullToEmpty(name.getNamespaceURI())));
        final String ln = name.getLocalPart();
        return (prefix.isEmpty() ? ln : (prefix + ":" + ln));
    }

    protected int write(String text, boolean compress) {
        final int originalLength = text.length();

        if (compress && whitespaceCompressor != null) {
            text = whitespaceCompressor.compress(text);
        }

        int written = 0;

        for (TextExtractorComponent component : components) {
            written += component.text(text);
        }

        if (offsetMapper != null) {
            offsetMapper.advance(written, originalLength);
        }

        offset += written;

        return written;
    }

    void map(Range<Integer> source, Range<Integer> text) {
        for (TextExtractorComponent component : components) {
            component.map(source, text);
        }
    }
}
