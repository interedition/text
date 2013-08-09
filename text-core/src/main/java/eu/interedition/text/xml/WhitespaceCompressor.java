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

package eu.interedition.text.xml;

import com.google.common.base.Objects;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamReader;
import java.util.Stack;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class WhitespaceCompressor extends ConversionFilter {

    private final Stack<Boolean> spacePreservationContext = new Stack<Boolean>();
    private final WhitespaceStrippingContext whitespaceStrippingContext;
    private char lastChar = ' ';

    public WhitespaceCompressor(WhitespaceStrippingContext whitespaceStrippingContext) {
        this.whitespaceStrippingContext = whitespaceStrippingContext;
    }

    @Override
    public void start() {
        spacePreservationContext.clear();
        whitespaceStrippingContext.reset();
        lastChar = ' ';
    }

    @Override
    protected void onXMLEvent(XMLStreamReader reader) {
        whitespaceStrippingContext.onXMLEvent(reader);

        if (reader.isStartElement()) {
            spacePreservationContext.push(spacePreservationContext.isEmpty() ? false : spacePreservationContext.peek());
            final Object xmlSpace = reader.getAttributeValue(XMLConstants.XML_NS_URI, "space");
            if (xmlSpace != null) {
                spacePreservationContext.pop();
                spacePreservationContext.push("preserve".equalsIgnoreCase(xmlSpace.toString()));
            }
        } else if (reader.isEndElement()) {
            spacePreservationContext.pop();
        }
    }

    String compress(String text) {
        final StringBuilder compressed = new StringBuilder();
        final boolean preserveSpace = Objects.firstNonNull(spacePreservationContext.peek(), false);
        for (int cc = 0, length = text.length(); cc < length; cc++) {
            char currentChar = text.charAt(cc);
            if (!preserveSpace && Character.isWhitespace(currentChar) && (Character.isWhitespace(lastChar) || whitespaceStrippingContext.isInContainerElement())) {
                continue;
            }
            if (currentChar == '\n' || currentChar == '\r') {
                currentChar = ' ';
            }
            compressed.append(lastChar = currentChar);
        }
        return compressed.toString();
    }

}
