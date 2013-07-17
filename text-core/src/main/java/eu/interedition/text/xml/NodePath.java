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

import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class NodePath extends LinkedList<Integer> implements StreamFilter {

    public NodePath reset() {
        clear();
        add(1);
        return this;
    }

    @Override
    public boolean accept(XMLStreamReader reader) {
        switch (reader.getEventType()) {
            case XMLStreamConstants.START_ELEMENT:
                add(removeLast() + 1);
                add(1);
                break;
            case XMLStreamConstants.END_ELEMENT:
                removeLast();
                break;
            case XMLStreamConstants.CDATA:
            case XMLStreamConstants.CHARACTERS:
            case XMLStreamConstants.COMMENT:
            case XMLStreamConstants.PROCESSING_INSTRUCTION:
            case XMLStreamConstants.SPACE:
                add(removeLast() + 1);
                break;
        }
        return true;
    }

    public List<Integer> elementPath() {
        return subList(0, size() - 1);
    }
}
