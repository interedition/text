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

package eu.interedition.text.repository;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.MoreExecutors;
import eu.interedition.text.Repository;
import eu.interedition.text.util.Database;
import org.codehaus.jackson.map.ObjectMapper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class JdbcRepository implements Repository {

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;
    private final Iterable<Listener> listeners;
    private final JdbcIdentifierGenerator textIds;
    private final JdbcIdentifierGenerator annotationIds;
    private ExecutorService listenerExecutorService = MoreExecutors.sameThreadExecutor();

    public JdbcRepository(DataSource dataSource, ObjectMapper objectMapper, Iterable<Listener> listeners) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
        this.listeners = listeners;
        this.textIds = new JdbcIdentifierGenerator(dataSource, "interedition_texts_id");
        this.annotationIds = new JdbcIdentifierGenerator(dataSource, "interedition_annotations_id");
    }

    public JdbcRepository(DataSource dataSource, ObjectMapper objectMapper, Listener... listeners) {
        this(dataSource, objectMapper, Arrays.asList(listeners));
    }

    public JdbcRepository withSchema() {
        textIds.withSchema();
        annotationIds.withSchema();
        return execute(new Transaction<JdbcRepository>() {
            @Override
            public JdbcRepository transactional(Store store) {
                ((JdbcStore) store).writeSchema();
                return JdbcRepository.this;
            }
        });
    }

    public JdbcRepository withListenerExecutorService(ExecutorService listenerExecutorService) {
        this.listenerExecutorService = listenerExecutorService;
        return this;
    }

    @Override
    public Iterator<Long> textIds() {
        return textIds;
    }

    @Override
    public Iterator<Long> annotationIds() {
        return annotationIds;
    }

    public <R> R execute(Transaction<R> transaction) {
        JdbcStore store = null;
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            connection.setReadOnly(transaction.isReadOnly());
            R result = transaction.transactional(store = new JdbcStore(connection, objectMapper));
            connection.commit();
            store.txLog().notify(listenerExecutorService, listeners);
            return result;
        } catch (Throwable t) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException e) {
                    throw Throwables.propagate(e);
                }
            }
            throw Throwables.propagate(t);
        } finally {
            if (store != null) {
                store.close();
            }
            Database.closeQuietly(connection);
        }

    }
}
