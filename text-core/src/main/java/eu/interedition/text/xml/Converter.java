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

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import eu.interedition.text.Segment;

import javax.xml.namespace.QName;
import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class Converter {

    private final WhitespaceCompressor whitespaceCompressor;
    private final OffsetMapper offsetMapper;
    private final Map<String, String> namespaceMapping;
    private final LinkedList<Integer> nodePath = Lists.newLinkedList();
    private final Iterable<StreamFilter> extractorChain;
    private final Iterable<ConversionFilter> textStreamFilters;
    private final List<ConverterListener> listeners = Lists.newArrayList();
    private int offset;

    public static ConverterBuilder builder() {
        return new ConverterBuilder();
    }

    public Converter add(ConverterListener... listeners) {
        return add(Arrays.asList(listeners));
    }

    public Converter add(Iterable<ConverterListener> listeners) {
        Iterables.addAll(this.listeners, listeners);
        return this;
    }

    public Converter remove(ConverterListener... listeners) {
        return remove(Arrays.asList(listeners));
    }

    public Converter remove(Collection<ConverterListener> listeners) {
        this.listeners.removeAll(listeners);
        return this;
    }

    public XMLStreamReader extract(XMLInputFactory inputFactory, XMLStreamReader source) throws XMLStreamException {
        offset = 0;

        for (ConverterListener listener : listeners) {
            listener.start();
        }

        for (ConversionFilter filter : Iterables.filter(extractorChain, ConversionFilter.class)) {
            filter.start();
        }

        final XMLStreamReader reader = XML.filter(inputFactory, source, extractorChain);
        while (reader.hasNext()) {
            reader.next();
        }

        for (ConversionFilter filter : Iterables.filter(extractorChain, ConversionFilter.class)) {
            filter.end();
        }

        for (ConverterListener listener : listeners) {
            listener.end();
        }

        return reader;
    }

    Converter(WhitespaceCompressor whitespaceCompressor,
              OffsetMapper offsetMapper,
              Map<String, String> namespaceMapping,
              Iterable<StreamFilter> extractorChain) {
        this.whitespaceCompressor = whitespaceCompressor;
        this.offsetMapper = offsetMapper;
        this.namespaceMapping = namespaceMapping;
        this.extractorChain = extractorChain;
        this.textStreamFilters = Iterables.filter(extractorChain, ConversionFilter.class);
        for (ConversionFilter streamFilter : this.textStreamFilters) {
            streamFilter.init(this);
        }
    }


    public int offset() {
        return offset;
    }

    public LinkedList<Integer> nodePath() {
        return nodePath;
    }

    public List<Integer> elementPath() {
        return nodePath.subList(0, nodePath.size() - 1);
    }

    public String insert(String text) {
        return write(text, false);
    }

    public String copy(String text) {
        return write(text, true);
    }

    public void annotationsStarts(Object data) {
        for (ConverterListener listener : listeners) {
            listener.annotationStart(offset, data);
        }
    }

    public void annotationEnds(int startOffset, Object data) {
        final Segment segment = new Segment(startOffset, offset);
        for (ConverterListener listener : listeners) {
            listener.annotationEnd(segment, data);
        }
    }

    public String name(QName name) {
        if (namespaceMapping == null) {
            return name.toString();
        }
        final String prefix = Strings.nullToEmpty(namespaceMapping.get(Strings.nullToEmpty(name.getNamespaceURI())));
        final String ln = name.getLocalPart();
        return (prefix.isEmpty() ? ln : (prefix + ":" + ln));
    }

    protected String write(String text, boolean compress) {
        final int originalLength = text.length();

        if (compress && whitespaceCompressor != null) {
            text = whitespaceCompressor.compress(text);
        }

        final StringBuilder buf = new StringBuilder();
        for (ConversionFilter component : textStreamFilters) {
            buf.append(component.text(text));
        }

        final String written = buf.toString();
        final int writtenLength = written.length();

        for (ConverterListener listener : listeners) {
            listener.text(offset, written);
        }

        if (offsetMapper != null) {
            offsetMapper.advance(writtenLength, originalLength);
        }

        offset += writtenLength;

        return written;
    }

    void map(Range<Integer> source, Range<Integer> text) {
        for (ConverterListener listener : listeners) {
            listener.map(source, text);
        }
    }
}
