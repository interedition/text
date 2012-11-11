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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import eu.interedition.text.Anchor;
import eu.interedition.text.Name;
import eu.interedition.text.TextConstants;
import eu.interedition.text.TextRange;
import eu.interedition.text.xml.XMLElementStart;
import eu.interedition.text.xml.XMLEntity;
import eu.interedition.text.xml.XMLTransformer;
import eu.interedition.text.xml.XMLTransformerConfiguration;
import java.util.Iterator;
import java.util.Map;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class TEIAwareAnnotationXMLTransformerModule<T> extends XMLTransformerModuleAdapter<T> {
    private static final Map<Name, Name> MILESTONE_ELEMENT_UNITS = Maps.newHashMap();

    static {
        MILESTONE_ELEMENT_UNITS.put(new Name(TextConstants.TEI_NS, "pb"), new Name(TextConstants.TEI_NS, "page"));
        MILESTONE_ELEMENT_UNITS.put(new Name(TextConstants.TEI_NS, "lb"), new Name(TextConstants.TEI_NS, "line"));
        MILESTONE_ELEMENT_UNITS.put(new Name(TextConstants.TEI_NS, "cb"), new Name(TextConstants.TEI_NS, "column"));
        MILESTONE_ELEMENT_UNITS.put(new Name(TextConstants.TEI_NS, "gb"), new Name(TextConstants.TEI_NS, "gathering"));
    }

    private static final Name MILESTONE_NAME = new Name(TextConstants.TEI_NS, "milestone");
    private static final Name MILESTONE_UNIT_ATTR_NAME = new Name(TextConstants.TEI_NS, "unit");

    private Multimap<String, XMLElementStart> spanning;
    private Map<Name, XMLElementStart> milestones;

    @Override
    public void start(XMLTransformer<T> transformer) {
        super.start(transformer);
        this.spanning = ArrayListMultimap.create();
        this.milestones = Maps.newHashMap();
    }

    @Override
    public void end(XMLTransformer<T> transformer) {
        final long textOffset = transformer.getTextOffset();
        for (Name milestoneUnit : milestones.keySet()) {
            final XMLElementStart last = milestones.get(milestoneUnit);

            final TextRange range = new TextRange(last.getOffset(), textOffset);
            final XMLTransformerConfiguration<T> configuration = transformer.getConfiguration();
            configuration.createAnnotation(last.getName(), last.getAttributes(), new Anchor(transformer.getTarget(), range));
        }

        this.milestones = null;
        this.spanning = null;

        super.end(transformer);
    }

    @Override
    public void start(XMLTransformer<T> transformer, XMLEntity entity) {
        super.start(transformer, entity);
        handleSpanningElements(entity, transformer);
        handleMilestoneElements(entity, transformer);
    }

    void handleMilestoneElements(XMLEntity entity, XMLTransformer<T> transformer) {
        final Name entityName = entity.getName();
        final Map<Name, Object> entityAttributes = entity.getAttributes();

        Name milestoneUnit = null;
        if (MILESTONE_NAME.equals(entityName)) {
            for (Iterator<Name> it = entityAttributes.keySet().iterator(); it.hasNext(); ) {
                final Name attrName = it.next();
                if (MILESTONE_UNIT_ATTR_NAME.getLocalName().equals(attrName.getLocalName()) ||
                        MILESTONE_UNIT_ATTR_NAME.toString().equals(attrName.getLocalName())) {
                    milestoneUnit = new Name(TextConstants.TEI_NS, entityAttributes.get(attrName).toString());
                    it.remove();
                }
            }
        } else if (MILESTONE_ELEMENT_UNITS.containsKey(entityName)) {
            milestoneUnit = MILESTONE_ELEMENT_UNITS.get(entityName);
        }

        if (milestoneUnit == null) {
            return;
        }

        final long textOffset = transformer.getTextOffset();

        final XMLElementStart last = milestones.get(milestoneUnit);
        if (last != null) {
            final TextRange range = new TextRange(last.getOffset(), textOffset);
            final XMLTransformerConfiguration<T> configuration = transformer.getConfiguration();
            configuration.createAnnotation(last.getName(), last.getAttributes(), new Anchor(transformer.getTarget(), range));

        }

        milestones.put(milestoneUnit, new XMLElementStart(milestoneUnit, entityAttributes, textOffset));
    }

    void handleSpanningElements(XMLEntity entity, XMLTransformer<T> transformer) {
        final Map<Name, Object> entityAttributes = entity.getAttributes();
        String spanTo = null;
        String refId = null;
        for (Iterator<Name> it = entityAttributes.keySet().iterator(); it.hasNext(); ) {
            final Name attrName = it.next();
            if (attrName.getLocalName().endsWith("spanTo")) {
                spanTo = entityAttributes.get(attrName).toString().replaceAll("^#", "");
                it.remove();
            } else if (TextConstants.XML_ID_ATTR_NAME.equals(attrName)) {
                refId = entityAttributes.get(attrName).toString();
            }
        }

        if (spanTo == null && refId == null) {
            return;
        }

        final long textOffset = transformer.getTextOffset();

        if (spanTo != null) {
            final Name name = entity.getName();
            spanning.put(spanTo, new XMLElementStart(
                    new Name(name.getNamespace(), name.getLocalName().replaceAll("Span$", "")),
                    entityAttributes,
                    textOffset));
        }
        if (refId != null) {
            for (XMLElementStart a : spanning.removeAll(refId)) {
                final TextRange range = new TextRange(a.getOffset(), textOffset);

                final XMLTransformerConfiguration<T> configuration = transformer.getConfiguration();
                configuration.createAnnotation(a.getName(), a.getAttributes(), new Anchor(transformer.getTarget(), range));
            }
        }
    }
}
