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

import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLOutputFactory2;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class XML {
    public static XMLInputFactory2 createXMLInputFactory() {
        final XMLInputFactory2 xmlInputFactory = (XMLInputFactory2) XMLInputFactory2.newInstance();
        xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
        xmlInputFactory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
        xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        xmlInputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.FALSE);
        xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
        return xmlInputFactory;
    }

    public static XMLOutputFactory2 createXMLOutputFactory() {
        final XMLOutputFactory2 xmlOutputFactory = (XMLOutputFactory2) XMLOutputFactory2.newInstance();
        xmlOutputFactory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);
        return xmlOutputFactory;
    }

    public static XMLStreamReader streamReaderChain(XMLStreamReader reader, Iterable<StreamFilter> filters) throws XMLStreamException {
        final XMLInputFactory2 xif = createXMLInputFactory();
        for (StreamFilter filter : filters) {
            reader = xif.createFilteredReader(reader, filter);
        }
        return reader;
    }

    public static XMLStreamReader streamReaderChain(XMLStreamReader reader, StreamFilter... filters) throws XMLStreamException {
        return streamReaderChain(reader, Arrays.asList(filters));
    }

    public static void toCharstream(Source source, Writer writer) throws SAXException, TransformerException {
        TransformerFactory.newInstance().newTransformer().transform(source, new StreamResult(writer));
    }

    public static Source source(InputSource inputSource) throws SAXException {
        final XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        xmlReader.setEntityResolver(new EntityResolver() {
            @Override
            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                return new InputSource(new StringReader(""));
            }
        });
        return new SAXSource(xmlReader, inputSource);
    }

    public static void closeQuietly(XMLStreamReader reader) {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (XMLStreamException e) {
        }
    }

    public static void closeQuietly(XMLEventReader reader) {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (XMLStreamException e) {
        }
    }

    public static void closeQuietly(XMLStreamWriter writer) {
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (XMLStreamException e) {
        }
    }

    public static void closeQuietly(XMLEventWriter writer) {
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (XMLStreamException e) {
        }
    }
}
