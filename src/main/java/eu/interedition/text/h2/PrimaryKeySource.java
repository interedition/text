package eu.interedition.text.h2;

import com.google.common.base.Preconditions;
import eu.interedition.text.util.AutoCloseables;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
class PrimaryKeySource implements Iterable<Long>, Iterator<Long> {

    private final H2TextRepository<?> repository;
    private final long low;
    private long next = -1;

    private PrimaryKeySource(H2TextRepository<?> repository, long low) {
        this.repository = repository;
        this.low = low;
    }

    public PrimaryKeySource(H2TextRepository<?> repository) {
        this(repository, 1024);
    }

    @Override
    public boolean hasNext() {
        return true;
    }

    public synchronized Long next() {
        if (++next % low == 0) {
            Connection connection = null;
            PreparedStatement nextStatement = null;
            ResultSet resultSet = null;
            try {
                connection = repository.begin();
                nextStatement = connection.prepareStatement("select interedition_text_id.nextval from dual");
                resultSet = nextStatement.executeQuery();
                Preconditions.checkState(resultSet.next());
                next = ((resultSet.getLong(1) - 1) * low) + 1;
                repository.commit(connection);
            } catch (SQLException e) {
                throw repository.rollbackAndConvert(connection, e);
            } finally {
                AutoCloseables.closeQuietly(resultSet);
                AutoCloseables.closeQuietly(nextStatement);
                AutoCloseables.closeQuietly(connection);
            }
        }
        return next;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<Long> iterator() {
        return this;
    }
}
