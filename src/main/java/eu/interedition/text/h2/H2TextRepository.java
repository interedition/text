package eu.interedition.text.h2;

import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;
import eu.interedition.text.Anchor;
import eu.interedition.text.Layer;
import eu.interedition.text.Name;
import eu.interedition.text.Query;
import eu.interedition.text.QueryResult;
import eu.interedition.text.Text;
import eu.interedition.text.TextRange;
import eu.interedition.text.TextRepository;
import eu.interedition.text.data.DataMapper;
import eu.interedition.text.data.SerializableDataMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import javax.sql.DataSource;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class H2TextRepository<T> implements TextRepository<T> {

    private final Class<T> dataType;
    private final DataSource ds;
    private final boolean transactional;

    final DataMapper<T> dataMapper = new SerializableDataMapper<T>();
    private final H2Query<T> query = new H2Query<T>();
    private final Iterator<Long> primaryKeySource = new PrimaryKeySource(this);

    public H2TextRepository(Class<T> dataType, DataSource ds) {
        this(dataType, ds, true);
    }

    public H2TextRepository(Class<T> dataType, DataSource ds, boolean transactional) {
        this.dataType = dataType;
        this.ds = ds;
        this.transactional = transactional;
    }

    public H2TextRepository<T> withSchema() {
        Connection connection = null;
        Statement stmt = null;
        try {
            connection = begin();
            stmt = connection.createStatement();
            stmt.executeUpdate("create sequence if not exists interedition_text_id");
            stmt.executeUpdate("create table if not exists interedition_name (id bigint primary key, ln varchar(100) not null, ns varchar(100), unique (ln, ns))");
            stmt.executeUpdate("create table if not exists interedition_text_layer (id bigint primary key, name_id bigint not null references interedition_name (id) on delete cascade, text_content clob not null, layer_data blob)");
            stmt.executeUpdate("create table if not exists interedition_text_anchor (from_id bigint not null references interedition_text_layer (id) on delete cascade, to_id bigint not null references interedition_text_layer (id) on delete cascade, range_start bigint not null, range_end bigint not null)");
            commit(connection);
            return this;
        } catch (SQLException e) {
            throw rollbackAndConvert(connection, e);
        } finally {
            SQL.closeQuietly(stmt);
            SQL.closeQuietly(connection);
        }
    }

    public Layer<T> byId(long id) {
        return query.byId(this, id);
    }

    @Override
    public QueryResult<T> query(Query query) {
        return this.query.results(this, query);
    }

    @Override
    public void delete(Iterable<Layer<T>> layers) {
        final StringBuilder deleteSql = new StringBuilder();
        for (LayerRelation<?> layer : Iterables.filter(layers, LayerRelation.class)) {
            final long id = layer.getId();
            if (deleteSql.length() == 0) {
                deleteSql.append("delete from interedition_text_layer where id in (").append(id);
            } else {
                deleteSql.append(",").append(id);
            }
        }
        if (deleteSql.length() > 0) {
            Connection connection = null;
            Statement deleteStatement = null;
            try {
                connection = begin();
                deleteStatement = connection.createStatement();
                deleteStatement.executeUpdate(deleteSql.append(")").toString());
            } catch (SQLException e) {
                throw rollbackAndConvert(connection, e);
            } finally {
                SQL.closeQuietly(deleteStatement);
                SQL.closeQuietly(connection);
            }
        }

    }

    @Override
    public Layer<T> add(Name name, Reader text, T data, Set<Anchor> anchors) throws IOException {
        Connection connection = null;
        PreparedStatement insertLayer = null;
        PreparedStatement insertTarget = null;
        try {
            connection = begin();
            insertLayer = connection.prepareStatement("insert into interedition_text_layer (name_id, text_content, layer_data, id) values (?, ?, ?, ?)");

            insertLayer.setLong(1, name(connection, name).getId());

            final Clob textClob = connection.createClob();
            Writer textWriter = null;
            try {
                CharStreams.copy(text, textWriter = textClob.setCharacterStream(1));
            } finally {
                Closeables.close(textWriter, false);
            }
            insertLayer.setClob(2, textClob);

            if (data != null) {
                final Blob dataBlob = connection.createBlob();
                OutputStream dataStream = null;
                try {
                    dataMapper.serialize(data, dataStream = dataBlob.setBinaryStream(1));
                } finally {
                    Closeables.close(dataStream, false);
                }
                insertLayer.setBlob(3, dataBlob);
            } else {
                insertLayer.setNull(3, Types.BLOB);
            }

            final long id = Iterators.getNext(primaryKeySource, null);
            insertLayer.setLong(4, id);

            insertLayer.executeUpdate();

            if (!anchors.isEmpty()) {
                insertTarget = connection.prepareStatement("insert into interedition_text_anchor (from_id, to_id, range_start, range_end) values (?, ?, ?, ?)");
            }
            for (Anchor anchor : anchors) {
                final Text anchorText = anchor.getText();
                if (anchorText instanceof LayerRelation) {
                    final TextRange anchorRange = anchor.getRange();
                    insertTarget.setLong(1, id);
                    insertTarget.setLong(2, ((LayerRelation<?>) anchorText).getId());
                    insertTarget.setLong(3, anchorRange.getStart());
                    insertTarget.setLong(4, anchorRange.getEnd());
                    insertTarget.executeUpdate();
                }
            }

            commit(connection);

            return new LayerRelation<T>(name, anchors, null, id, this);
        } catch (SQLException e) {
            throw rollbackAndConvert(connection, e);
        } finally {
            SQL.closeQuietly(insertTarget);
            SQL.closeQuietly(insertLayer);
            SQL.closeQuietly(connection);
        }
    }

    @Override
    public Layer<T> add(Name name, Reader text, T data, Anchor... anchors) throws IOException {
        return add(name, text, data, Sets.newHashSet(Arrays.asList(anchors)));
    }

    NameRelation name(Connection connection, Name name) throws SQLException {
        if (name instanceof NameRelation) {
            return (NameRelation) name;
        }

        final String ln = name.getLocalName();
        final URI ns = name.getNamespace();

        PreparedStatement findName = null;
        PreparedStatement insertName = null;
        ResultSet resultSet = null;
        try {
            findName = connection.prepareStatement("select id from interedition_name where ln = ? and ns = ?");

            findName.setString(1, ln);
            if (ns == null) {
                findName.setNull(2, Types.VARCHAR);
            } else {
                findName.setString(2, ns.toString());
            }
            resultSet = findName.executeQuery();
            if (resultSet.next()) {
                return new NameRelation(ns, ln, resultSet.getLong(1));
            }

            insertName = connection.prepareStatement("insert into interedition_name (id, ln, ns) values (?, ?, ?)");
            final long id = Iterators.getNext(primaryKeySource, null);
            insertName.setLong(1, id);
            insertName.setString(2, ln);
            insertName.setString(3, ns == null ? null : ns.toString());
            insertName.executeUpdate();

            return new NameRelation(name, id);
        } finally {
            SQL.closeQuietly(resultSet);
            SQL.closeQuietly(insertName);
            SQL.closeQuietly(findName);
        }
    }

    Connection begin() throws SQLException {
        final Connection connection = ds.getConnection();
        if (transactional) {
            connection.setAutoCommit(false);
        }
        return connection;
    }

    void commit(Connection connection) throws SQLException {
        if (transactional) {
            connection.commit();
        }
    }

    RuntimeException rollbackAndConvert(Connection connection, Throwable t) {
        if (connection != null && transactional) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
            }
        }
        return Throwables.propagate(t);
    }

    public T data(InputStream stream) throws IOException {
        return dataMapper.deserialize(stream, dataType);
    }
}
