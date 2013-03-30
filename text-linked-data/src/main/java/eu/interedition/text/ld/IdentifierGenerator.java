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

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import eu.interedition.text.ld.util.Database;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class IdentifierGenerator {

    private final DataSource dataSource;
    private final String sequence;
    private final long low;
    private long next = -1;

    private IdentifierGenerator(DataSource dataSource, String sequence, long low) {
        this.dataSource = dataSource;
        this.sequence = sequence;
        this.low = low;
    }

    public IdentifierGenerator(DataSource dataSource, String sequence) {
        this(dataSource, sequence, 1024);
    }

    public IdentifierGenerator withSchema() {
        Connection connection = null;
        Statement stmt = null;
        try {
            connection = dataSource.getConnection();
            stmt = connection.createStatement();
            stmt.executeUpdate("create sequence if not exists " + sequence);
            connection.commit();
            return this;
        } catch (SQLException e) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                }
            }
            throw Throwables.propagate(e);
        } finally {
            Database.closeQuietly(stmt);
            Database.closeQuietly(connection);
        }
    }
    public synchronized long next() {
        if (++next % low == 0) {
            Connection connection = null;
            PreparedStatement nextStatement = null;
            ResultSet resultSet = null;
            try {
                connection = dataSource.getConnection();
                nextStatement = connection.prepareStatement("select " + sequence + ".nextval from dual");
                resultSet = nextStatement.executeQuery();
                Preconditions.checkState(resultSet.next());
                next = ((resultSet.getLong(1) - 1) * low) + 1;
                connection.commit();
            } catch (Throwable t) {
                if (connection != null) {
                    try {
                        connection.rollback();
                    } catch (SQLException e1) {
                    }
                }
                throw Throwables.propagate(t);
            } finally {
                Database.closeQuietly(resultSet);
                Database.closeQuietly(nextStatement);
                Database.closeQuietly(connection);
            }
        }
        return next;
    }
}
