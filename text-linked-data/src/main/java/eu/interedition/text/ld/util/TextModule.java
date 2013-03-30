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

package eu.interedition.text.ld.util;

import eu.interedition.text.ld.Annotation;
import eu.interedition.text.ld.AnnotationTarget;
import eu.interedition.text.ld.Segment;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.module.SimpleModule;

import java.io.IOException;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class TextModule extends SimpleModule {

    public TextModule() {
        super("text", Version.unknownVersion());
        addSerializer(Segment.class, new SegmentSerializer());
        addSerializer(Annotation.class, new AnnotationSerializer());
    }

    public static class AnnotationSerializer extends JsonSerializer<Annotation> {
        @Override
        public void serialize(Annotation value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
            jgen.writeStartObject();
            jgen.writeObjectField("id", value.id());
            jgen.writeArrayFieldStart("targets");
            for (AnnotationTarget target : value.targets()) {
                jgen.writeStartArray();
                jgen.writeNumber(target.text());
                jgen.writeNumber(target.start());
                jgen.writeNumber(target.end());
                jgen.writeEndArray();
            }
            jgen.writeEndArray();
            jgen.writeObjectField("data", value.data());
            jgen.writeEndObject();
        }
    }

    public class SegmentSerializer extends JsonSerializer<Segment> {
        @Override
        public void serialize(Segment value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
            jgen.writeStartArray();
            jgen.writeNumber(value.start());
            jgen.writeNumber(value.end());
            jgen.writeEndArray();
        }
    }
}
