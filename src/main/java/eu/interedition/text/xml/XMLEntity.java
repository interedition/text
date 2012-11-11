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

import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import eu.interedition.text.Anchor;
import eu.interedition.text.Name;
import eu.interedition.text.Text;
import eu.interedition.text.TextConstants;
import eu.interedition.text.TextRange;
import eu.interedition.text.simple.SimpleLayer;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamReader;

import static eu.interedition.text.TextConstants.XML_NS_URI;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class XMLEntity {
  private static final Name COMMENT_QNAME = new Name(XML_NS_URI, "comment");

  private static final Name PI_QNAME = new Name(XML_NS_URI, "pi");

  private static final Name PI_TARGET_ATTR = new Name(TextConstants.XML_NS_URI, "piTarget");
  private static final Name PI_DATA_ATTR = new Name(TextConstants.XML_NS_URI, "piData");

  private final String prefix;
  private final Name name;
  private final Map<Name, Object> attributes;


  private XMLEntity(Name name, String prefix) {
    this(name, prefix, new HashMap<Name, Object>());
  }

  private XMLEntity(Name name, String prefix, Map<Name, Object> attributes) {
    this.name = name;
    this.prefix = prefix;
    this.attributes = attributes;
  }

  public String getPrefix() {
    return prefix;
  }

  public Name getName() {
    return name;
  }

  public Map<Name, Object> getAttributes() {
    return attributes;
  }

  public static XMLEntity newComment(XMLStreamReader reader) {
    return new XMLEntity(COMMENT_QNAME, XMLConstants.DEFAULT_NS_PREFIX);
  }

  public static XMLEntity newPI(XMLStreamReader reader) {
    final Map<Name, Object> attributes = new HashMap<Name, Object>();
    attributes.put(PI_TARGET_ATTR, reader.getPITarget());

    final String data = reader.getPIData();
    if (data != null) {
      attributes.put(PI_DATA_ATTR, data);
    }
    return new XMLEntity(PI_QNAME, XMLConstants.DEFAULT_NS_PREFIX, attributes);
  }

  public static XMLEntity newElement(XMLStreamReader reader) {
    return new XMLEntity(new Name(reader.getName()), XMLConstants.DEFAULT_NS_PREFIX, attributesToData(reader));
  }

  private static Map<Name, Object> attributesToData(XMLStreamReader reader) {
    final int attributeCount = reader.getAttributeCount();
    final Map<Name, Object> attributes = Maps.newHashMapWithExpectedSize(attributeCount);
    for (int ac = 0; ac < attributeCount; ac++) {
      attributes.put(new Name(reader.getAttributeName(ac)), reader.getAttributeValue(ac));
    }
    return attributesToData(attributes);
  }

  private static Map<Name, Object> attributesToData(Map<Name, Object> attributes) {
    final Map<Name, Object> data = new HashMap<Name, Object>();
    for (Map.Entry<Name, Object> attribute : attributes.entrySet()) {
      final URI namespace = attribute.getKey().getNamespace();
      if (namespace != null && XMLNS_ATTRIBUTE_NS_URI.equals(namespace.toString())) {
        continue;
      }
      data.put(attribute.getKey(), attribute.getValue());
    }
    return data;
  }

  public SimpleLayer<Map<Name, Object>> toAnnotation(Text text, long offset) {
    return new SimpleLayer<Map<Name, Object>>(name, "", attributes, new Anchor(text, new TextRange(offset, offset)));
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this).addValue(name).toString();
  }
}
