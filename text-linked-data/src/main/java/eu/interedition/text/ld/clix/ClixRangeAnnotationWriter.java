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

package eu.interedition.text.ld.clix;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import eu.interedition.text.ld.Annotation;
import eu.interedition.text.ld.AnnotationTarget;
import eu.interedition.text.ld.IdentifierGenerator;
import eu.interedition.text.ld.Store;
import eu.interedition.text.ld.xml.AnnotationWriter;
import eu.interedition.text.ld.xml.NamespaceMapping;
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
public class ClixRangeAnnotationWriter extends AnnotationWriter {

    static final String CLIX_START_ATTR_NAME = "sID";
    static final String CLIX_END_ATTR_NAME = "eID";

    private final long text;
    final IdentifierGenerator annotationIds;
    final ObjectMapper objectMapper;

    final Deque<Boolean> clixElements = new ArrayDeque<Boolean>();
    final Map<String, Annotation> annotations = Maps.newHashMap();
    final Map<String, Integer> startOffsets = Maps.newHashMap();

    public ClixRangeAnnotationWriter(Store texts, long text, IdentifierGenerator annotationIds) {
        super(texts);
        this.text = text;
        this.annotationIds = annotationIds;
        this.objectMapper = texts.objectMapper();
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
                    attributes.put(extractor().name(name), reader.getAttributeValue(a));
                }
                final ObjectNode data = objectMapper.createObjectNode();
                data.put(extractor().name(XML_ELEMENT_NAME), extractor().name(reader.getName()));
                if (attributes.size() > 0) {
                    data.put(extractor().name(XML_ELEMENT_ATTRS), attributes);
                }
                annotations.put(startId, new Annotation(annotationIds.next(), Sets.<AnnotationTarget>newTreeSet(), data));
                startOffsets.put(startId, extractor().offset());
            }
            if (endId != null) {
                final Annotation annotation = annotations.remove(endId);
                final Integer start = startOffsets.remove(endId);
                if (annotation != null && start != null) {
                    write(new Annotation(
                            annotation.id(),
                            new AnnotationTarget(text, start, extractor().offset()),
                            annotation.data()
                    ));
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
