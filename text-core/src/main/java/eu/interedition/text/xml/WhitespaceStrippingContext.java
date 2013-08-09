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

import javax.xml.stream.XMLStreamReader;
import java.util.Set;
import java.util.Stack;

/**
* @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
*/
public class WhitespaceStrippingContext {

    public boolean isInContainerElement() {
        return false;
    }

    public void onXMLEvent(XMLStreamReader reader) {
    }

    public void reset() {
    }

    public static class ElementNameBased extends WhitespaceStrippingContext {

        private final Set<String> containerElements;
        private Stack<Boolean> containerElementStack = new Stack<Boolean>();

        public ElementNameBased(Set<String> containerElements) {
            this.containerElements = containerElements;
        }

        @Override
        public boolean isInContainerElement() {
            return (!containerElementStack.empty() && containerElementStack.peek());
        }

        @Override
        public void onXMLEvent(XMLStreamReader reader) {
            if (reader.isStartElement()) {
                containerElementStack.push(containerElements.contains(reader.getLocalName()));
            } else if (reader.isEndElement()) {
                containerElementStack.pop();
            }
        }

        @Override
        public void reset() {
            containerElementStack.clear();
        }
    }
}
