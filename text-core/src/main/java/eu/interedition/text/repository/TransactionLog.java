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

package eu.interedition.text.repository;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import eu.interedition.text.Repository;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class TransactionLog {

    private final List<Long> addedTexts = Lists.newLinkedList();
    private final List<Long> addedAnnotations = Lists.newLinkedList();
    private final List<Long> removedTexts = Lists.newLinkedList();
    private final List<Long> removedAnnotations = Lists.newLinkedList();


    public void textsAdded(Long... ids) {
        textsAdded(Arrays.asList(ids));
    }

    public void textsAdded(Collection<Long> ids) {
        addedTexts.addAll(ids);
        removedTexts.removeAll(ids);
    }

    public void textsRemoved(Long... ids) {
        textsRemoved(Arrays.asList(ids));
    }

    public void textsRemoved(Collection<Long> ids) {
        removedTexts.addAll(ids);
        addedTexts.removeAll(ids);
    }

    public void annotationsAdded(Long... ids) {
        annotationsAdded(Arrays.asList(ids));
    }

    public void annotationsAdded(Collection<Long> ids) {
        addedAnnotations.addAll(ids);
        removedAnnotations.removeAll(ids);
    }

    public void annotationsRemoved(Long... ids) {
        annotationsRemoved(Arrays.asList(ids));
    }

    public void annotationsRemoved(Collection<Long> ids) {
        removedAnnotations.addAll(ids);
        addedAnnotations.removeAll(ids);
    }

    public TransactionLog clear() {
        addedTexts.clear();
        removedTexts.clear();
        addedAnnotations.clear();
        removedAnnotations.clear();
        return this;
    }

    public TransactionLog notify(ExecutorService executorService, Iterable<Repository.Listener> listeners) {
        if (!Iterables.isEmpty(listeners)) {
            final boolean added = !(addedAnnotations.isEmpty() && addedTexts.isEmpty());
            final boolean removed = !(removedAnnotations.isEmpty() && removedTexts.isEmpty());
            if (added || removed) {
                final Long[] addedTexts = this.addedTexts.toArray(new Long[this.addedTexts.size()]);
                final Long[] addedAnnotations = this.addedAnnotations.toArray(new Long[this.addedAnnotations.size()]);
                final Long[] removedTexts = this.removedTexts.toArray(new Long[this.removedTexts.size()]);
                final Long[] removedAnnotations = this.removedAnnotations.toArray(new Long[this.removedAnnotations.size()]);
                for (final Repository.Listener listener : listeners) {
                    executorService.submit(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            if (added) {
                                listener.added(addedTexts, addedAnnotations);
                            }
                            if (removed) {
                                listener.removed(removedTexts, removedAnnotations);
                            }
                            return null;
                        }
                    });
                }
            }
        }
        return this;
    }
}
