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

package eu.interedition.text.repository;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import eu.interedition.text.Annotation;
import eu.interedition.text.AnnotationTarget;
import eu.interedition.text.Segment;
import eu.interedition.text.repository.Store;
import eu.interedition.text.xml.ConverterListener;
import org.codehaus.jackson.JsonNode;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class StoringConverterListener extends ConverterListener.Adapter {
    private static final int DEFAULT_BATCH_SIZE = 1024;

    private final Store store;
    private final Iterator<Long> annotationIds;
    private final long textId;
    private final Writer textWriter;
    private final int batchSize;

    private final List<Annotation> batch = Lists.newArrayListWithCapacity(DEFAULT_BATCH_SIZE);

    public StoringConverterListener(Store store, Iterator<Long> annotationIds, long textId, Writer textWriter) {
        this(store, annotationIds, textId, textWriter, DEFAULT_BATCH_SIZE);
    }

    public StoringConverterListener(Store store, Iterator<Long> annotationIds, long textId, Writer textWriter, int batchSize) {
        this.store = store;
        this.annotationIds = annotationIds;
        this.textId = textId;
        this.textWriter = textWriter;
        this.batchSize = batchSize;
    }

    @Override
    public void annotationEnd(Segment range, Object data) {
        batch.add(new Annotation(annotationIds.next(), new AnnotationTarget(textId, range.start(), range.end()), (JsonNode) data));
        if (batch.size() >= batchSize) {
            store();
        }
    }

    @Override
    public void text(int offset, String text) {
        try {
            textWriter.write(text);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void end() {
        store();
    }

    public void store() {
        if (!batch.isEmpty()) {
            store.annotate(batch);
            batch.clear();
        }
    }

}
