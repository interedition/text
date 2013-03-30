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

package eu.interedition.text.ld.xml;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import eu.interedition.text.ld.Annotation;
import eu.interedition.text.ld.AnnotationTarget;
import eu.interedition.text.ld.IdentifierGenerator;
import eu.interedition.text.ld.Store;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public abstract class AnnotationWriter extends TextExtractorComponent {
    private static final int BATCH_SIZE = 1024;

    protected final Store texts;

    public AnnotationWriter(Store texts) {
        this.texts = texts;
    }

    @Override
    protected void onXMLEvent(XMLStreamReader reader) {
        if (reader.getEventType() == XMLStreamConstants.END_DOCUMENT) {
            flush();
        }
    }

    protected void write(Annotation... annotations) {
        write(Arrays.asList(annotations));
    }

    protected void write(Iterable<Annotation> annotations) {
        Iterables.addAll(this.annotations, annotations);
        if (this.annotations.size() >= BATCH_SIZE) {
            flush();
        }
    }

    public static class Elements extends AnnotationWriter {

        private final Deque<Annotation> annotations = new ArrayDeque<Annotation>();
        private final Deque<Integer> offsets = new ArrayDeque<Integer>();
        private final IdentifierGenerator annotationIds;
        private final long text;
        private final ObjectMapper objectMapper;

        public Elements(Store store, long text, IdentifierGenerator annotationIds) {
            super(store);
            this.text = text;
            this.objectMapper = store.objectMapper();
            this.annotationIds = annotationIds;
        }

        @Override
        protected void onXMLEvent(XMLStreamReader reader) {
            final int offset = extractor().offset();
            if (reader.isStartElement()) {
                final ObjectNode data = objectMapper.createObjectNode();
                data.put("xml:name", extractor().name(reader.getName()));
                final int ac = reader.getAttributeCount();
                if (ac > 0) {
                    final ObjectNode attributes = data.putObject("xml:attrs");
                    for (int a = 0; a < ac; a++) {
                        attributes.put(extractor().name(reader.getAttributeName(a)), reader.getAttributeValue(a));
                    }
                }
                annotations.push(new Annotation(annotationIds.next(), Sets.<AnnotationTarget>newTreeSet(), data));
                offsets.push(offset);
            } else if (reader.isEndElement()) {
                final Annotation started = annotations.pop();
                write(new Annotation(started.id(), new AnnotationTarget(text, offsets.pop(), offset), started.data()));
            }
            super.onXMLEvent(reader);
        }
    }

    private void flush() {
        if (!annotations.isEmpty()) {
            texts.annotate(annotations);
            annotations.clear();
        }
    }

    private final List<Annotation> annotations = Lists.newArrayListWithCapacity(BATCH_SIZE);
}
