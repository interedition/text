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

package eu.interedition.text.ld;

import com.google.common.base.Objects;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class AnnotationTarget extends Segment {

    private final long text;

    public AnnotationTarget(long text, int start, int end) {
        super(start, end);
        this.text = text;
    }

    public long text() {
        return text;
    }

    @Override
    public int compareTo(Segment o) {
        if (o instanceof AnnotationTarget) {
            final long result = text - ((AnnotationTarget) o).text;
            if (result != 0) {
                return (result < 0 ? -1 : 1);
            }
        }
        return super.compareTo(o);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(text, super.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof AnnotationTarget) {
            final AnnotationTarget other = (AnnotationTarget) obj;
            if (text != other.text) {
                return false;
            }
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return text + super.toString();
    }
}
