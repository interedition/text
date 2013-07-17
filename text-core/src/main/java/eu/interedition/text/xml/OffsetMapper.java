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

import com.google.common.collect.Range;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class OffsetMapper extends TextExtractorComponent {
    private static final Logger LOG = Logger.getLogger(OffsetMapper.class.getName());

    private int sourceOffset = 0;
    private Range<Integer> sourceOffsetRange = Range.closedOpen(0, 0);
    private Range<Integer> textOffsetRange = Range.closedOpen(0, 0);

    public OffsetMapper reset(TextExtractor extractor) {
        this.extractor = extractor;
        this.sourceOffset = 0;
        this.sourceOffsetRange = Range.closedOpen(0, 0);
        this.textOffsetRange = Range.closedOpen(0, 0);
        return this;
    }

    @Override
    protected void onXMLEvent(XMLStreamReader reader) {
        final int sourceOffset = reader.getLocation().getCharacterOffset();
        add(0, sourceOffset - this.sourceOffset);
        this.sourceOffset = sourceOffset;

        if (reader.getEventType() == XMLStreamConstants.END_DOCUMENT) {
            map();
        }
    }

    public void advance(int text, int source) {
        add(text, source);
    }

    private void add(int addToText, int addToSource) {
        if (addToText == 0 && addToSource == 0) {
            return;
        }

        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer("Moving offsets: text += " + addToText + "; source += " + addToSource);
        }

        final int textOffsetRangeLength = textOffsetRange.upperEndpoint() - textOffsetRange.lowerEndpoint();
        final int sourceOffsetRangeLength = sourceOffsetRange.upperEndpoint() - sourceOffsetRange.lowerEndpoint();

        if (addToText == 0 && textOffsetRangeLength == 0) {
            sourceOffsetRange = Range.closedOpen(sourceOffsetRange.lowerEndpoint(), sourceOffsetRange.upperEndpoint() + addToSource);
        } else if (addToSource == 0 && sourceOffsetRangeLength == 0) {
            textOffsetRange = Range.closedOpen(textOffsetRange.lowerEndpoint(), textOffsetRange.upperEndpoint() + addToText);
        } else if (textOffsetRangeLength == sourceOffsetRangeLength && addToText == addToSource) {
            sourceOffsetRange = Range.closedOpen(sourceOffsetRange.lowerEndpoint(), sourceOffsetRange.upperEndpoint() + addToSource);
            textOffsetRange = Range.closedOpen(textOffsetRange.lowerEndpoint(), textOffsetRange.upperEndpoint() + addToText);
        } else {
            map();
            sourceOffsetRange = Range.closedOpen(sourceOffsetRange.upperEndpoint(), sourceOffsetRange.upperEndpoint() + addToSource);
            textOffsetRange = Range.closedOpen(textOffsetRange.upperEndpoint(), textOffsetRange.upperEndpoint() + addToText);
        }
    }

    private void map() {
        if (!textOffsetRange.isEmpty() || !sourceOffsetRange.isEmpty()) {
            if (LOG.isLoggable(Level.FINER)) {
                LOG.finer("New offset mapping: text = " + textOffsetRange + "==> source += " + sourceOffsetRange);
            }

            extractor().map(sourceOffsetRange, textOffsetRange);
        }
    }
}
