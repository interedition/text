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

package eu.interedition.text.ld.util;

import com.google.common.base.Preconditions;
import org.h2.jdbcx.JdbcConnectionPool;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class Database {

    private static final Logger LOG = Logger.getLogger(Database.class.getName());

    public static JdbcConnectionPool h2() {
        return h2("mem:texts;DB_CLOSE_DELAY=-1");
    }

    public static JdbcConnectionPool h2(File directory) {
        Preconditions.checkArgument(directory.isDirectory() || directory.mkdirs(), directory);
        return h2(new File(directory, "texts").getPath() + ";DB_CLOSE_DELAY=10");
    }

    protected static JdbcConnectionPool h2(String url) {
        return JdbcConnectionPool.create("jdbc:h2:" + url, "sa", "");
    }

    public static void closeQuietly(Connection connection) {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            if (LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, MessageFormat.format("Exception while closing connection {0}", connection), e);
            }
        }
    }


    public static void closeQuietly(ResultSet resultSet) {
        try {
            if (resultSet != null) {
                resultSet.close();
            }
        } catch (SQLException e) {
            if (LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, MessageFormat.format("Exception while closing result set {0}", resultSet), e);
            }
        }
    }

    public static void closeQuietly(PreparedStatement stmt) {
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException e) {
            if (LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, MessageFormat.format("Exception while closing prepared statement {0}", stmt), e);
            }
        }
    }

    public static void closeQuietly(Statement stmt) {
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException e) {
            if (LOG.isLoggable(Level.WARNING)) {
                LOG.log(Level.WARNING, MessageFormat.format("Exception while closing statement {0}", stmt), e);
            }
        }
    }
}
