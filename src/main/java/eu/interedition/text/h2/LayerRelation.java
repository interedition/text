package eu.interedition.text.h2;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;
import eu.interedition.text.Anchor;
import eu.interedition.text.Layer;
import eu.interedition.text.Name;
import eu.interedition.text.TextRange;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class LayerRelation<T> implements Layer<T> {

    private final Name name;
    private final Set<Anchor> anchors;
    private final long id;
    private final H2TextRepository<T> repository;

    public LayerRelation(Name name, Set<Anchor> anchors, long id, H2TextRepository<T> repository) {
        this.name = name;
        this.anchors = anchors;
        this.id = id;
        this.repository = repository;
    }

    public long getId() {
        return id;
    }

    @Override
    public Name getName() {
        return name;
    }

    @Override
    public Set<Anchor> getAnchors() {
        return anchors;
    }

    @Override
    public void read(Writer target) throws IOException {
        read(null, target);
    }

    @Override
    public void read(final TextRange range, final Writer target) throws IOException {
        withTextClob(new ClobCallback<Void>() {
            @Override
            public Void withClob(Clob text) throws IOException, SQLException {
                Reader content = (range == null ? text.getCharacterStream() : text.getCharacterStream(range.getStart() + 1, range.length()));
                try {
                    CharStreams.copy(content, target);
                } finally {
                    Closeables.close(content, false);
                }
                return null;
            }
        });
    }

    @Override
    public Reader read() throws IOException {
        return read((TextRange) null);
    }

    @Override
    public Reader read(TextRange range) throws IOException {
        final StringWriter buf = new StringWriter();
        read(range, buf);
        return new StringReader(buf.toString());
    }


    @Override
    public T data(Class<T> type) {
        Connection connection = null;
        PreparedStatement query = null;
        ResultSet resultSet = null;
        try {
            connection = repository.begin();
            query = connection.prepareStatement("SELECT l.layer_data FROM interedition_text_layer l WHERE l.id = ?");
            query.setLong(1, id);
            resultSet = query.executeQuery();
            Preconditions.checkArgument(resultSet.next(), this);

            T result = null;
            final Blob dataBlob = resultSet.getBlob(1);
            if (dataBlob != null) {
                InputStream dataStream = null;
                try {
                    result = repository.data(dataStream = dataBlob.getBinaryStream(), type);
                } finally {
                    Closeables.close(dataStream, false);
                }
            }
            repository.commit(connection);
            return result;
        } catch (SQLException e) {
            throw repository.rollbackAndConvert(connection, e);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        } finally {
            SQL.closeQuietly(resultSet);
            SQL.closeQuietly(query);
            SQL.closeQuietly(connection);
        }
    }

    @Override
    public SortedMap<TextRange, String> read(final SortedSet<TextRange> textRanges) {
        try {
            return withTextClob(new ClobCallback<SortedMap<TextRange, String>>() {
                @Override
                public SortedMap<TextRange, String> withClob(Clob text) throws IOException, SQLException {
                    final SortedMap<TextRange, String> result = Maps.newTreeMap();
                    for (TextRange range : textRanges) {
                        result.put(range, text.getSubString(range.getStart() + 1, (int) range.length()));
                    }
                    return result;
                }
            });
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public long length() {
        try {
            return withTextClob(new ClobCallback<Long>() {
                @Override
                public Long withClob(Clob text) throws IOException, SQLException {
                    return text.length();
                }
            });
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private interface ClobCallback<R> {
        R withClob(Clob text) throws IOException, SQLException;
    }

    private <R> R withTextClob(ClobCallback<R> callback) throws IOException {
        Connection connection = null;
        PreparedStatement query = null;
        ResultSet resultSet = null;
        try {
            connection = repository.begin();
            query = connection.prepareStatement("SELECT l.text_content FROM interedition_text_layer l WHERE l.id = ?");
            query.setLong(1, id);
            resultSet = query.executeQuery();
            Preconditions.checkArgument(resultSet.next(), this);
            final Clob clob = resultSet.getClob(1);
            final R result = callback.withClob(clob);
            clob.free();
            repository.commit(connection);
            return result;
        } catch (SQLException e) {
            throw repository.rollbackAndConvert(connection, e);
        } finally {
            SQL.closeQuietly(resultSet);
            SQL.closeQuietly(query);
            SQL.closeQuietly(connection);
        }
    }
}
