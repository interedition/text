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

package eu.interedition.text.lmnl;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import eu.interedition.text.xml.AnnotationGenerator;
import eu.interedition.text.xml.NamespaceMapping;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class ClixRangeAnnotationGenerator extends AnnotationGenerator {

    static final String CLIX_START_ATTR_NAME = "sID";
    static final String CLIX_END_ATTR_NAME = "eID";

    final Deque<Boolean> clixElements = new ArrayDeque<Boolean>();
    final Map<String, ObjectNode> annotations = Maps.newHashMap();
    final Map<String, Integer> startOffsets = Maps.newHashMap();

    public ClixRangeAnnotationGenerator(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public boolean accept(XMLStreamReader reader) {
        if (reader.isStartElement()) {
            String startId = null;
            String endId = null;
            for (int a = 0, ac = reader.getAttributeCount(); a < ac; a++) {
                final String localName = reader.getAttributeLocalName(a);
                final String ns = Strings.nullToEmpty(reader.getAttributeNamespace(a));
                if (!ns.isEmpty() && !ns.equals(NamespaceMapping.CLIX_NS_URI)) {
                    continue;
                }
                if (CLIX_START_ATTR_NAME.equals(localName)) {
                    startId = reader.getAttributeValue(a);
                } else if (CLIX_END_ATTR_NAME.equals(localName)) {
                    endId = reader.getAttributeValue(a);
                }
            }
            if (startId == null && endId == null) {
                clixElements.push(false);
                return true;
            }

            if (startId != null) {
                final ObjectNode attributes = objectMapper.createObjectNode();
                for (int a = 0, ac = reader.getAttributeCount(); a < ac; a++) {
                    final QName name = reader.getAttributeName(a);
                    final String ns = Strings.nullToEmpty(name.getNamespaceURI());
                    final String ln = name.getLocalPart();
                    if ((ns.isEmpty() || ns.equals(NamespaceMapping.CLIX_NS_URI)) && (CLIX_START_ATTR_NAME.equals(ln) || CLIX_END_ATTR_NAME.equals(ln))) {
                        continue;
                    }
                    attributes.put(converter().name(name), reader.getAttributeValue(a));
                }
                final ObjectNode data = objectMapper.createObjectNode();
                data.put(converter().name(XML_ELEMENT_NAME), converter().name(reader.getName()));
                if (attributes.size() > 0) {
                    data.put(converter().name(XML_ELEMENT_ATTRS), attributes);
                }
                annotations.put(startId, data);
                startOffsets.put(startId, converter().offset());
                converter().annotationsStarts(data);
            }
            if (endId != null) {
                final ObjectNode data = annotations.remove(endId);
                final Integer start = startOffsets.remove(endId);
                if (data != null && start != null) {
                    converter().annotationEnds(start, data);
                }
            }
            clixElements.push(true);
            return false;
        } else if (reader.isEndElement()) {
            return !clixElements.pop();
        }
        return true;
    }
}
