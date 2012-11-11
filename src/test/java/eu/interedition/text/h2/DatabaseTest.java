package eu.interedition.text.h2;

import eu.interedition.text.AbstractTest;
import eu.interedition.text.Name;
import eu.interedition.text.simple.KeyValues;
import java.io.IOException;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.Collections;
import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class DatabaseTest extends AbstractTest {

    private static H2TextRepository<KeyValues> repository;
    private static SingleConnectionDataSource ds;

    @Test
    public void create() throws SQLException, IOException {
        repository().add(new Name(TEST_NS, "dbLayer"), new StringReader("Hello World"), null);
    }

    @Test
    public void createDelete() throws SQLException, IOException {
        final H2TextRepository<KeyValues> repository = repository();
        repository.delete(Collections.singleton(repository.add(new Name(TEST_NS, "dbLayer"), new StringReader("Hello World"), null)));
    }

    private static H2TextRepository<KeyValues> repository() throws SQLException {
        if (repository == null) {
            final StringBuilder url = new StringBuilder("jdbc:h2:mem:text;DB_CLOSE_DELAY=-1");
            if (System.getProperty("interedition.debug") != null) {
                url.append(";TRACE_LEVEL_SYSTEM_OUT=2");
            }
            ds = new SingleConnectionDataSource(JdbcConnectionPool.create(url.toString(), "sa", "sa"));
            repository = new H2TextRepository<KeyValues>(ds, false).withSchema();
        }
        return repository;
    }

    @AfterClass
    public static void closeDataSource() {
        repository = null;
        if (ds != null && ds.connection != null) {
            SQL.closeQuietly(ds.connection);
        }
    }

    @After
    public void rollback() throws SQLException {
        if (ds != null && ds.connection != null) {
            ds.connection.rollback();
        }
    }


}
