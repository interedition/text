package eu.interedition.text.h2;

import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
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
import eu.interedition.text.util.BackupSupport;
import eu.interedition.text.util.BatchLayerAdditionSupport;
import eu.interedition.text.util.UpdateSupport;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.Charset;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import javax.sql.DataSource;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class H2TextRepository<T> implements TextRepository<T>, UpdateSupport<T>, BackupSupport, BatchLayerAdditionSupport<T> {

    private final Class<T> dataType;
    private final DataSource ds;
    private final boolean transactional;

    private final DataMapper<T> dataMapper = new SerializableDataMapper<T>();

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
            stmt.executeUpdate("create table if not exists interedition_text_anchor (id bigint primary key, from_id bigint not null references interedition_text_layer (id) on delete cascade, to_id bigint not null references interedition_text_layer (id) on delete cascade, range_start bigint not null, range_end bigint not null)");
            stmt.executeUpdate("create index if not exists interedition_text_range on interedition_text_anchor (range_start, range_end)");
            commit(connection);
            return this;
        } catch (SQLException e) {
            throw rollbackAndConvert(connection, e);
        } finally {
            JdbcUtil.closeQuietly(stmt);
            JdbcUtil.closeQuietly(connection);
        }
    }

    public Layer<T> findByIdentifier(long id) {
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
                JdbcUtil.closeQuietly(deleteStatement);
                JdbcUtil.closeQuietly(connection);
            }
        }

    }

    @Override
    public Iterable<Layer<T>> add(Iterable<Layer<T>> batch) throws IOException {
        final List<Layer<T>> added = Lists.newLinkedList();

        Connection connection = null;
        PreparedStatement insertLayer = null;
        PreparedStatement insertAnchor = null;
        try {
            for (Layer<T> layer : batch) {
                if (connection == null) {
                    connection = begin();
                }
                if (insertLayer == null) {
                    insertLayer = connection.prepareStatement("insert into interedition_text_layer (name_id, text_content, layer_data, id) values (?, ?, ?, ?)");
                }

                final NameRelation nameRelation = name(layer.getName());
                insertLayer.setLong(1, nameRelation.getId());

                final Clob textClob = connection.createClob();
                Writer textWriter = null;
                try {
                    layer.read(textWriter = textClob.setCharacterStream(1));
                } finally {
                    Closeables.close(textWriter, false);
                }
                insertLayer.setClob(2, textClob);

                final T data = layer.data();
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

                insertLayer.addBatch();

                final Set<Anchor> anchors = layer.getAnchors();
                final Set<Anchor> mappedAnchors = Sets.newHashSet();
                for (Anchor anchor : anchors) {
                    final Text anchorText = anchor.getText();
                    if (anchorText instanceof LayerRelation) {
                        if (insertAnchor == null) {
                            insertAnchor = connection.prepareStatement("insert into interedition_text_anchor (id, from_id, to_id, range_start, range_end) values (?, ?, ?, ?, ?)");
                        }
                        final long anchorId = Iterators.getNext(primaryKeySource, null);
                        final TextRange anchorRange = anchor.getRange();
                        insertAnchor.setLong(1, anchorId);
                        insertAnchor.setLong(2, id);
                        insertAnchor.setLong(3, ((LayerRelation<?>) anchorText).getId());
                        insertAnchor.setLong(4, anchorRange.getStart());
                        insertAnchor.setLong(5, anchorRange.getEnd());
                        insertAnchor.addBatch();
                        mappedAnchors.add(new AnchorRelation(anchorText, anchorRange, anchorId));
                    }
                }

                added.add(new LayerRelation<T>(nameRelation, mappedAnchors, data, id, this));
            }

            if (insertLayer != null) {
                insertLayer.executeBatch();
            }
            if (insertAnchor != null) {
                insertAnchor.executeBatch();
            }
            if (connection != null) {
                commit(connection);
            }

            return added;
        } catch (SQLException e) {
            throw rollbackAndConvert(connection, e);
        } finally {
            JdbcUtil.closeQuietly(insertAnchor);
            JdbcUtil.closeQuietly(insertLayer);
            JdbcUtil.closeQuietly(connection);
        }
    }

    @Override
    public Layer<T> add(final Name name, final Reader text, final T data, final Set<Anchor> anchors) throws IOException {
        return Iterables.getOnlyElement(add(Collections.<Layer<T>>singleton(new Layer<T>() {
            @Override
            public Set<Anchor> getAnchors() {
                return anchors;
            }

            @Override
            public T data() {
                return data;
            }

            @Override
            public Name getName() {
                return name;
            }

            @Override
            public void read(Writer target) throws IOException {
                CharStreams.copy(text, target);
            }

            @Override
            public void read(TextRange range, Writer target) throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void stream(Consumer consumer) throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void stream(TextRange range, Consumer consumer) throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public String read() throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public String read(TextRange range) throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public SortedMap<TextRange, String> read(SortedSet<TextRange> textRanges) {
                throw new UnsupportedOperationException();
            }

            @Override
            public long length() {
                throw new UnsupportedOperationException();
            }
        })));
    }

    @Override
    public void updateText(Layer<T> target, Reader text) throws IOException {
        if (target instanceof LayerRelation) {
            Connection connection = null;
            PreparedStatement update = null;
            try {
                connection = begin();
                update = connection.prepareStatement("UPDATE interedition_text_layer SET text_content = ? WHERE id = ?");

                final Clob textClob = connection.createClob();
                Writer textStream = null;
                try {
                    CharStreams.copy(text, textStream = textClob.setCharacterStream(1));
                } finally {
                    Closeables.close(textStream, false);
                }

                update.setClob(1, textClob);
                update.setLong(2, ((LayerRelation<?>) target).getId());
                update.executeUpdate();

            } catch (SQLException e) {
                throw rollbackAndConvert(connection, e);
            } finally {
                JdbcUtil.closeQuietly(update);
                JdbcUtil.closeQuietly(connection);
            }
        }
    }

    @Override
    public Layer<T> add(Name name, Reader text, T data, Anchor... anchors) throws IOException {
        return add(name, text, data, Sets.newHashSet(Arrays.asList(anchors)));
    }

    public void backup(Writer to) throws IOException {
        Connection connection = null;
        PreparedStatement script = null;
        ResultSet resultSet = null;
        try {
            connection = begin();
            script = connection.prepareStatement("SCRIPT DROP BLOCKSIZE 10485760");
            resultSet = script.executeQuery();
            while (resultSet.next()) {
                final Reader scriptReader = resultSet.getCharacterStream(1);
                try {
                    CharStreams.copy(scriptReader, to);
                } finally {
                    Closeables.close(scriptReader, false);
                }
                to.write("\n");
            }
            commit(connection);
        } catch (SQLException e) {
            throw rollbackAndConvert(connection, e);
        } finally {
            JdbcUtil.closeQuietly(resultSet);
            JdbcUtil.closeQuietly(script);
            JdbcUtil.closeQuietly(connection);
        }
    }

    @Override
    public void restore(File from, Charset charset) throws IOException {
        Connection connection = null;
        Statement runScript = null;
        ResultSet resultSet = null;
        try {
            connection = begin();
            runScript = connection.createStatement();
            runScript.executeUpdate(String.format("RUNSCRIPT FROM '%s' CHARSET '%s'", from.getPath(), charset.name()));
            commit(connection);
        } catch (SQLException e) {
            throw rollbackAndConvert(connection, e);
        } finally {
            JdbcUtil.closeQuietly(resultSet);
            JdbcUtil.closeQuietly(runScript);
            JdbcUtil.closeQuietly(connection);
        }
    }

    @Override
    public void restore(Reader from) throws IOException {
        final File restoreSql = File.createTempFile(getClass().getName() + ".restore", ".sql");
        restoreSql.deleteOnExit();

        try {
            final Charset charset = Charset.forName("UTF-8");
            Writer tempWriter = null;
            try {
                CharStreams.copy(from, tempWriter = new OutputStreamWriter(new FileOutputStream(restoreSql), charset));
            } finally {
                Closeables.close(tempWriter, false);
            }
            restore(restoreSql, charset);
        } finally {
            restoreSql.delete();
        }
    }

    NameRelation cachedName(Long id, final NameRelation name) {
        try {
            nameCache.put(name, name);
            return nameIdCache.get(id, new Callable<NameRelation>() {
                @Override
                public NameRelation call() throws Exception {
                    return name;
                }
            });
        } catch (ExecutionException e) {
            throw Throwables.propagate(e);
        }
    }

    NameRelation name(final Name name) throws SQLException {
        if (name instanceof NameRelation) {
            return (NameRelation) name;
        }
        try {
            return nameCache.get(name, new Callable<NameRelation>() {
                @Override
                public NameRelation call() throws Exception {
                    final String ln = name.getLocalName();
                    final URI ns = name.getNamespace();

                    Connection connection = null;
                    PreparedStatement findName = null;
                    PreparedStatement insertName = null;
                    ResultSet resultSet = null;
                    try {
                        connection = begin();

                        findName = connection.prepareStatement("select id from interedition_name where ln = ? and ns = ?");
                        findName.setString(1, ln);
                        if (ns == null) {
                            findName.setNull(2, Types.VARCHAR);
                        } else {
                            findName.setString(2, ns.toString());
                        }
                        resultSet = findName.executeQuery();

                        NameRelation result;
                        if (resultSet.next()) {
                            result = new NameRelation(name, resultSet.getLong(1));
                        } else {
                            insertName = connection.prepareStatement("insert into interedition_name (id, ln, ns) values (?, ?, ?)");
                            final long id = Iterators.getNext(primaryKeySource, null);
                            insertName.setLong(1, id);
                            insertName.setString(2, ln);
                            insertName.setString(3, ns == null ? null : ns.toString());
                            insertName.executeUpdate();
                            result = new NameRelation(name, id);
                        }

                        commit(connection);

                        nameIdCache.put(result.getId(), result);
                        return result;
                    } catch(SQLException e) {
                        throw rollbackAndConvert(connection, e);
                    } finally {
                        JdbcUtil.closeQuietly(resultSet);
                        JdbcUtil.closeQuietly(insertName);
                        JdbcUtil.closeQuietly(findName);
                        JdbcUtil.closeQuietly(connection);
                    }
                }
            });
        } catch (ExecutionException e) {
            Throwables.propagateIfInstanceOf(Throwables.getRootCause(e), SQLException.class);
            throw Throwables.propagate(e);
        }
    }

    public void clearNameCache() {
        nameCache.invalidateAll();
        nameIdCache.invalidateAll();
    }

    private final Cache<Name, NameRelation> nameCache = CacheBuilder.newBuilder().build();
    private final Cache<Long, NameRelation> nameIdCache = CacheBuilder.newBuilder().build();

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
            } catch (SQLException e) {
            }
        }
        return Throwables.propagate(t);
    }

    public T data(InputStream stream) throws IOException {
        return dataMapper.deserialize(stream, dataType);
    }
}
