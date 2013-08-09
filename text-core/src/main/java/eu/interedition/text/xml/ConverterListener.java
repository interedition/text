/*
 * Copyright (c) 2013 The Interedition Development Group.
 *
 * This file is part of Interedition Text.
 *
 * Interedition Text is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Interedition Text is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CollateX.  If not, see <http://www.gnu.org/licenses/>.
 */

package eu.interedition.text.xml;

import com.google.common.collect.Range;
import eu.interedition.text.Segment;
import org.codehaus.jackson.JsonNode;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public interface ConverterListener {

    void start();

    void annotationStart(int offset, Object data);

    void annotationEnd(Segment range, Object data);

    void text(int offset, String text);

    void end();

    void map(Range<Integer> source, Range<Integer> text);

    public static class Adapter implements ConverterListener {

        @Override
        public void start() {
        }

        @Override
        public void annotationStart(int offset, Object data) {
        }

        @Override
        public void annotationEnd(Segment range, Object data) {
        }

        @Override
        public void text(int offset, String text) {
        }

        @Override
        public void end() {
        }

        @Override
        public void map(Range<Integer> source, Range<Integer> text) {
        }
    }
}
