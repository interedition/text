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

package eu.interedition.text;

import com.google.common.base.Objects;
import com.google.common.collect.Range;

import java.io.FilterReader;
import java.io.IOException;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class Segment implements Comparable<Segment> {

    private final Range<Integer> range;

    public Segment(int start, int end) {
        this.range = Range.closedOpen(start, end);
    }

    public int start() {
        return this.range.lowerEndpoint();
    }

    public int end() {
        return this.range.upperEndpoint();
    }

    public int length() {
        return end() - start();
    }

    public Range<Integer> range() {
        return range;
    }

    @Override
    public int compareTo(Segment o) {
        int result = start() - o.start();
        if (result == 0) {
            result = end() - o.end();
        }
        return result;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(start(), end());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof Segment) {
            final Segment other = (Segment) obj;
            return (start() == other.start()) && (end() == other.end());
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return range.toString();
    }

    public static class Reader extends FilterReader {

        private final int start;
        private final int end;
        private int offset = 0;

        public Reader(java.io.Reader in, Segment range) {
            super(in);
            this.start = range.start();
            this.end = range.end();
        }

        @Override
        public int read() throws IOException {
            while (offset < start) {
                final int read = doRead();
                if (read < 0) {
                    return read;
                }
            }
            if (offset >= end) {
                return -1;
            }

            return doRead();
        }

        protected int doRead() throws IOException {
            final int read = super.read();
            if (read >= 0) {
                ++offset;
            }
            return read;
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            int read = 0;
            int last;
            while ((read < len) && ((last = read()) >= 0)) {
                cbuf[off + read++] = (char) last;
            }
            return ((len > 0 && read == 0) ? -1 : read);
        }
    }
}
