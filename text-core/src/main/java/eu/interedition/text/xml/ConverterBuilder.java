/*
 * Copyright (c) 2013 The Interedition Development Group.
 *
 * This file is part of Interedition Text.
 *
 * Interedition Text is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Interedition Text is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CollateX.  If not, see <http://www.gnu.org/licenses/>.
 */

package eu.interedition.text.xml;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class ConverterBuilder {

    private WhitespaceCompressor whitespaceCompressor;
    private OffsetMapper offsetMapper;
    private NodePathTracker nodePathTracker;
    private Map<String, String> namespaceMapping;
    private final List<StreamFilter> filters = Lists.newLinkedList();
    private final List<ConverterListener> listeners = Lists.newLinkedList();

    public ConverterBuilder withWhitespaceCompression() {
        return withWhitespaceCompression(new WhitespaceStrippingContext());
    }

    public ConverterBuilder withWhitespaceCompression(WhitespaceStrippingContext whitespaceStrippingContext) {
        this.whitespaceCompressor = new WhitespaceCompressor(whitespaceStrippingContext);
        return this;
    }

    public ConverterBuilder withOffsetMapping() {
        this.offsetMapper = new OffsetMapper();
        return this;
    }

    public ConverterBuilder withNodePath() {
        this.nodePathTracker = new NodePathTracker();
        return this;
    }

    public ConverterBuilder withNamespaceMapping(Map<String, String> namespaceMapping) {
        this.namespaceMapping = namespaceMapping;
        return this;
    }

    public ConverterBuilder filter(Iterable<StreamFilter> filters) {
        Iterables.addAll(this.filters, filters);
        return this;
    }

    public ConverterBuilder filter(StreamFilter... filters) {
        return filter(Arrays.asList(filters));
    }

    public ConverterBuilder listener(ConverterListener... listeners) {
        return listener(Arrays.asList(listeners));
    }

    public ConverterBuilder listener(Iterable<ConverterListener> listeners) {
        Iterables.addAll(this.listeners, listeners);
        return this;
    }

    public Converter build() {
        final List<StreamFilter> filters = Lists.newLinkedList();

        if (nodePathTracker != null) {
            filters.add(nodePathTracker);
        }

        Iterable<ConversionFilter> textStreamFilters = Iterables.filter(this.filters, ConversionFilter.class);

        Iterables.addAll(filters, Iterables.filter(textStreamFilters, ConversionFilter.BEFORE_TEXT_GENERATION));

        if (offsetMapper != null) {
            filters.add(offsetMapper);
        }
        if (whitespaceCompressor != null) {
            filters.add(whitespaceCompressor);
        }

        Iterables.addAll(filters, Iterables.filter(textStreamFilters, Predicates.not(ConversionFilter.BEFORE_TEXT_GENERATION)));
        Iterables.addAll(filters, Iterables.filter(this.filters, Predicates.not(Predicates.instanceOf(ConversionFilter.class))));

        return new Converter(whitespaceCompressor, offsetMapper, namespaceMapping, filters).add(listeners);
    }

    private static class NodePathTracker extends ConversionFilter {

        @Override
        public void start() {
            final LinkedList<Integer> nodePath = converter().nodePath();
            nodePath.clear();
            nodePath.add(1);
        }

        @Override
        protected void onXMLEvent(XMLStreamReader reader) {
            final LinkedList<Integer> nodePath = converter().nodePath();
            switch (reader.getEventType()) {
                case XMLStreamConstants.START_ELEMENT:
                    nodePath.add(nodePath.removeLast() + 1);
                    nodePath.add(1);
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    nodePath.removeLast();
                    break;
                case XMLStreamConstants.CDATA:
                case XMLStreamConstants.CHARACTERS:
                case XMLStreamConstants.COMMENT:
                case XMLStreamConstants.PROCESSING_INSTRUCTION:
                case XMLStreamConstants.SPACE:
                    nodePath.add(nodePath.removeLast() + 1);
                    break;
            }
        }
    }
}
