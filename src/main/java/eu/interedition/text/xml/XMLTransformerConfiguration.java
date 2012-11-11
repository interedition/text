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
package eu.interedition.text.xml;

import eu.interedition.text.Anchor;
import eu.interedition.text.Layer;
import eu.interedition.text.Name;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;

public interface XMLTransformerConfiguration<T> {

    boolean isLineElement(XMLEntity entity);

    boolean isContainerElement(XMLEntity entity);

    boolean included(XMLEntity entity);

    boolean excluded(XMLEntity entity);

    char getNotableCharacter();

    boolean isNotable(XMLEntity entity);

    boolean isCompressingWhitespace();

    List<XMLTransformerModule<T>> getModules();

    int getTextBufferSize();

    boolean isRemoveLeadingWhitespace();

    Layer<T> targetFor(Layer<T> source);

    void write(Layer<T> target, Reader text) throws IOException;

    Layer<T> xmlElement(Name name, Map<Name, Object> attributes, Anchor... anchors);
}
