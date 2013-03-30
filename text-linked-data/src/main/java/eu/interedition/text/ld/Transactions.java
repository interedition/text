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

import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import eu.interedition.text.ld.util.Database;
import org.codehaus.jackson.map.ObjectMapper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class Transactions {

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;
    private final Iterable<StoreListener> listeners;

    public Transactions(DataSource dataSource, ObjectMapper objectMapper, Iterable<StoreListener> listeners) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
        this.listeners = listeners;
    }

    public Transactions(DataSource dataSource, ObjectMapper objectMapper, StoreListener... listeners) {
        this(dataSource, objectMapper, Arrays.asList(listeners));
    }

    public <R> R execute(Transaction<R> transaction) throws SQLException {
        Store store = null;
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            connection.setReadOnly(transaction.isReadOnly());
            R result = transaction.withStore(store = new Store(connection, objectMapper));
            connection.commit();
            notifyListeners(store);
            return result;
        } catch (Throwable t) {
            if (connection != null) {
                connection.rollback();
            }
            Throwables.propagateIfInstanceOf(Throwables.getRootCause(t), SQLException.class);
            throw Throwables.propagate(t);
        } finally {
            if (store != null) {
                store.close();
            }
            Database.closeQuietly(connection);
        }

    }

    private void notifyListeners(Store store) {
        if (Iterables.isEmpty(listeners)) {
            return;
        }
        final boolean added = !(store.addedAnnotations.isEmpty() && store.addedTexts.isEmpty());
        final boolean removed = !(store.removedAnnotations.isEmpty() && store.removedTexts.isEmpty());
        if (added || removed) {
            final Iterable<Long> addedTexts = Iterables.unmodifiableIterable(store.addedTexts);
            final Iterable<Long> addedAnnotations = Iterables.unmodifiableIterable(store.addedAnnotations);
            final Iterable<Long> removedTexts = Iterables.unmodifiableIterable(store.removedTexts);
            final Iterable<Long> removedAnnotations = Iterables.unmodifiableIterable(store.removedAnnotations);
            for (StoreListener listener : listeners) {
                if (added) {
                    listener.added(addedTexts, addedAnnotations);
                }
                if (removed) {
                    listener.added(removedTexts, removedAnnotations);
                }
            }
        }
    }

    public static abstract class Transaction<R> {
        protected abstract R withStore(Store store) throws SQLException;

        public boolean isReadOnly() {
            return false;
        }
    }
}
