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
package eu.interedition.text.xml.module;

import com.google.common.collect.Maps;
import eu.interedition.text.Anchor;
import eu.interedition.text.Name;
import eu.interedition.text.TextConstants;
import eu.interedition.text.TextRange;
import eu.interedition.text.xml.XMLElementStart;
import eu.interedition.text.xml.XMLEntity;
import eu.interedition.text.xml.XMLTransformer;

import java.util.Collections;
import java.util.Map;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class CLIXAnnotationXMLTransformerModule<T> extends XMLTransformerModuleAdapter<T> {
    private Map<String, XMLElementStart> ranges;

    @Override
    public void start(XMLTransformer<T> transformer) {
        super.start(transformer);
        ranges = Maps.newHashMap();
    }

    @Override
    public void end(XMLTransformer<T> transformer) {
        ranges = null;
        super.end(transformer);
    }

    @Override
    public void start(XMLTransformer<T> transformer, XMLEntity entity) {
        super.start(transformer, entity);

        final Map<Name, Object> entityAttributes = entity.getAttributes();
        final Object startId = entityAttributes.remove(TextConstants.CLIX_START_ATTR_NAME);
        final Object endId = entityAttributes.remove(TextConstants.CLIX_END_ATTR_NAME);
        if (startId == null && endId == null) {
            return;
        }

        final long textOffset = transformer.getTextOffset();

        if (startId != null) {
            ranges.put(startId.toString(), new XMLElementStart(entity.getName(), entityAttributes, textOffset));
        }
        if (endId != null) {
            final XMLElementStart a = ranges.remove(endId.toString());
            if (a != null) {
                final TextRange range = new TextRange(a.getOffset(), textOffset);
                final Anchor<T> anchor = new Anchor<T>(transformer.getTarget(), range);
                transformer.getConfiguration().xmlElement(a.getName(), a.getAttributes(), anchor);
            }
        }
    }
}
