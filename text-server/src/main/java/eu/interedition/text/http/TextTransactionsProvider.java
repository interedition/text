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

package eu.interedition.text.http;

import com.google.common.base.Throwables;
import eu.interedition.text.Store;
import eu.interedition.text.Transactions;
import org.codehaus.jackson.map.ObjectMapper;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
@Singleton
public class TextTransactionsProvider implements Provider<Transactions> {

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;

    @Inject
    public TextTransactionsProvider(DataSource dataSource, ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
    }

    @Override
    public Transactions get() {
        final Transactions transactions = new Transactions(dataSource, objectMapper);
        try {
            transactions.execute(new Transactions.Transaction<Object>() {
                @Override
                protected Object withStore(Store store) throws SQLException {
                    store.withSchema();
                    return null;
                }
            });
            return transactions;
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        }
    }
}
