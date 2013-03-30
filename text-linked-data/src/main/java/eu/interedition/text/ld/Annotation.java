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

import com.google.common.collect.Sets;
import org.codehaus.jackson.JsonNode;

import java.util.Collections;
import java.util.SortedSet;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class Annotation {

    private final long id;
    private final SortedSet<AnnotationTarget> targets;
    private final JsonNode data;

    public Annotation(long id, SortedSet<AnnotationTarget> targets, JsonNode data) {
        this.id = id;
        this.targets = targets;
        this.data = data;
    }

    public Annotation(long id, AnnotationTarget target, JsonNode data) {
        this(id, Sets.newTreeSet(Collections.singleton(target)), data);
    }

    public long id() {
        return id;
    }

    public SortedSet<AnnotationTarget> targets() {
        return targets;
    }

    public JsonNode data() {
        return data;
    }
}
