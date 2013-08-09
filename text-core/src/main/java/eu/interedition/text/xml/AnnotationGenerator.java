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

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public abstract class AnnotationGenerator extends ConversionFilter {
    public static final QName XML_ELEMENT_NAME = new QName(XMLConstants.XML_NS_URI, "name");
    public static final QName XML_ELEMENT_ATTRS = new QName(XMLConstants.XML_NS_URI, "attributes");
    public static final QName XML_NODE_PATH = new QName(XMLConstants.XML_NS_URI, "path");

    protected final ObjectMapper objectMapper;

    protected AnnotationGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    protected ObjectNode annotationData(XMLStreamReader reader) {
        final ObjectNode data = objectMapper.createObjectNode();
        data.put(converter().name(XML_ELEMENT_NAME), converter().name(reader.getName()));
        final int ac = reader.getAttributeCount();
        if (ac > 0) {
            final ObjectNode attributes = data.putObject(converter().name(XML_ELEMENT_ATTRS));
            for (int a = 0; a < ac; a++) {
                attributes.put(converter().name(reader.getAttributeName(a)), reader.getAttributeValue(a));
            }
        }
        if (!converter().nodePath().isEmpty()) {
            data.put(converter().name(XML_NODE_PATH), objectMapper.valueToTree(converter().elementPath()));
        }
        return data;

    }

    public static class Elements extends AnnotationGenerator {

        private final Deque<ObjectNode> annotations = new ArrayDeque<ObjectNode>();
        private final Deque<Integer> startOffsets = new ArrayDeque<Integer>();
        public Elements(ObjectMapper objectMapper) {
            super(objectMapper);
        }

        @Override
        protected void onXMLEvent(XMLStreamReader reader) {
            final int offset = converter().offset();
            if (reader.isStartElement()) {
                final ObjectNode data = annotationData(reader);
                annotations.push(data);
                startOffsets.push(offset);
                converter().annotationsStarts(data);
            } else if (reader.isEndElement()) {
                converter().annotationEnds(startOffsets.pop(), annotations.pop());
            }
        }
    }

}
