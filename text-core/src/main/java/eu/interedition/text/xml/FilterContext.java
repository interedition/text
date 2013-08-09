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
public abstract class FilterContext extends ConversionFilter {

    private final Stack<Boolean> filterContext = new Stack<Boolean>();

    @Override
    protected boolean callBeforeTextGeneration() {
        return true;
    }

    @Override
    public boolean accept(XMLStreamReader reader) {
        if (contextStart(reader)) {
            final boolean parentIncluded = (filterContext.isEmpty() ? true : filterContext.peek());
            filterContext.push(parentIncluded ? !excluded(reader) : included(reader));
        }

        final boolean accept = (filterContext.isEmpty() || filterContext.peek());

        if (contextEnd(reader) && !filterContext.isEmpty()) {
            filterContext.pop();
        }

        return accept;
    }

    protected abstract boolean contextStart(XMLStreamReader reader);

    protected abstract boolean contextEnd(XMLStreamReader reader);

    protected abstract boolean included(XMLStreamReader reader);

    protected abstract boolean excluded(XMLStreamReader reader);

    public static class ElementNameBased extends FilterContext {

        private final Set<String> includedNames;
        private final Set<String> excludedNames;

        public ElementNameBased(Set<String> includedNames, Set<String> excludedNames) {
            this.includedNames = includedNames;
            this.excludedNames = excludedNames;
        }

        @Override
        protected boolean contextStart(XMLStreamReader reader) {
            return reader.isStartElement();
        }

        @Override
        protected boolean contextEnd(XMLStreamReader reader) {
            return reader.isEndElement();
        }

        @Override
        protected boolean included(XMLStreamReader reader) {
            return includedNames.contains(reader.getLocalName());
        }

        @Override
        protected boolean excluded(XMLStreamReader reader) {
            return excludedNames.contains(reader.getLocalName());
        }
    }
}
