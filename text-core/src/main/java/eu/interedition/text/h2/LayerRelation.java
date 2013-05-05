package eu.interedition.text.h2;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;
import eu.interedition.text.Anchor;
import eu.interedition.text.Layer;
import eu.interedition.text.Name;
import eu.interedition.text.Query;
import eu.interedition.text.QueryResult;
import eu.interedition.text.TextRange;
import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
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
    private final Set<Anchor<T>> anchors;
    private final T data;
    private final long id;
    private final H2TextRepository<T> repository;

    public LayerRelation(Name name, Set<Anchor<T>> anchors, T data, long id, H2TextRepository<T> repository) {
        this.name = name;
        this.anchors = anchors;
        this.data = data;
        this.id = id;
        this.repository = repository;
    }

    @Override
    public long getId() {
        return id;
    }

	@Override
    public Name getName() {
        return name;
    }

    @Override
    public Set<Anchor<T>> getAnchors() {
        return anchors;
    }

	@Override
	public Set<Layer<T>> getPorts() throws IOException {
		final QueryResult<T> qr = repository.query(Query.text(this));
		try {
			final Set<Layer<T>> ports = Sets.newHashSet();
			Iterables.addAll(ports, qr);
			return ports;
		} finally {
			Closeables.closeQuietly(qr);
		}
	}

	@Override
    public void read(Writer target) throws IOException {
        read(null, target);
    }

    @Override
    public void stream(Consumer consumer) throws IOException {
         stream(null, consumer);
    }

    @Override
    public void stream(final TextRange range, final Consumer consumer) throws IOException {
        withTextClob(new ClobCallback<Void>() {
            @Override
            public Void withClob(Clob text) throws IOException, SQLException {
                Reader content = text.getCharacterStream();
                if (range != null) {
                    content = new RangeFilteringReader(content, range);
                }
                try {
                    consumer.consume(content);
                } finally {
                    Closeables.close(content, false);
                }
                return null;
            }
        });
    }

    @Override
    public void read(final TextRange range, final Writer target) throws IOException {
        stream(range, new Consumer() {
            @Override
            public void consume(Reader text) throws IOException {
                CharStreams.copy(text, target);
            }
        });
    }

    @Override
    public String read() throws IOException {
        return read((TextRange) null);
    }

    @Override
    public String read(TextRange range) throws IOException {
        final StringWriter buf = new StringWriter();
        read(range, buf);
        return buf.toString();
    }


    @Override
    public T data() {
        if (data != null) {
            return data;
        }
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
                    result = repository.data(dataStream = dataBlob.getBinaryStream());
                } finally {
                    Closeables.close(dataStream, false);
                    dataBlob.free();
                }
            }
            repository.commit(connection);
            return result;
        } catch (SQLException e) {
            throw repository.rollbackAndConvert(connection, e);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        } finally {
            JdbcUtil.closeQuietly(resultSet);
            JdbcUtil.closeQuietly(query);
            JdbcUtil.closeQuietly(connection);
        }
    }

    @Override
    public SortedMap<TextRange, String> read(final SortedSet<TextRange> textRanges) {
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
    }

    @Override
    public long length() {
        return withTextClob(new ClobCallback<Long>() {
            @Override
            public Long withClob(Clob text) throws IOException, SQLException {
                return text.length();
            }
        });
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof LayerRelation) {
            LayerRelation<?> other = (LayerRelation<?>) obj;
            return (id == other.id);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).addValue(name).addValue(Iterables.toString(anchors)).addValue(id).toString();
    }

    private interface ClobCallback<R> {
        R withClob(Clob text) throws IOException, SQLException;
    }

    private <R> R withTextClob(ClobCallback<R> callback) {
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
        } catch (IOException e) {
            throw repository.rollbackAndConvert(connection, e);
        } finally {
            JdbcUtil.closeQuietly(resultSet);
            JdbcUtil.closeQuietly(query);
            JdbcUtil.closeQuietly(connection);
        }
    }

    private static class RangeFilteringReader extends FilterReader {

        private final TextRange range;
        private int offset = 0;

        public RangeFilteringReader(Reader in, TextRange range) {
            super(in);
            this.range = range;
        }

        @Override
        public int read() throws IOException {
            while (offset < range.getStart()) {
                final int read = doRead();
                if (read < 0) {
                    return read;
                }
            }
            if (offset >= range.getEnd()) {
                return -1;
            }

            return doRead();
        }

        protected int doRead() throws IOException {
            final int read = super.read();
            if (read >= 0) {
                ++offset;
            }
            return read;
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            int read = 0;
            int last;
            while ((read < len) && ((last = read()) >= 0)) {
                cbuf[off + read++] = (char) last;
            }
            return ((len > 0 && read == 0) ? -1 : read);
        }
    }
}
