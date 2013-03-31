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
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;
import eu.interedition.text.ld.util.Database;
import eu.interedition.text.ld.xml.TextExtractor;
import eu.interedition.text.ld.xml.TextExtractorComponent;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;
import org.codehaus.jackson.map.ObjectWriter;

import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class Store {

    private final Connection connection;
    private final ObjectMapper objectMapper;

    private PreparedStatement selectTexts;
    private PreparedStatement selectAnnotations;
    private PreparedStatement insertText;
    private PreparedStatement insertAnnotation;
    private PreparedStatement insertTarget;
    private PreparedStatement deleteTexts;

    List<Long> addedTexts = Lists.newLinkedList();
    List<Long> addedAnnotations = Lists.newLinkedList();
    List<Long> removedTexts = Lists.newLinkedList();
    List<Long> removedAnnotations = Lists.newLinkedList();

    public Store(Connection connection, ObjectMapper objectMapper) {
        this.connection = connection;
        this.objectMapper = objectMapper;
    }

    public void close() {
        Database.closeQuietly(deleteTexts);
        Database.closeQuietly(insertTarget);
        Database.closeQuietly(insertAnnotation);
        Database.closeQuietly(insertText);
        Database.closeQuietly(selectAnnotations);
        Database.closeQuietly(selectTexts);
    }

    public ObjectMapper objectMapper() {
        return objectMapper;
    }

    public <R> R browse(TextBrowser<R> browser) {
        ResultSet resultSet = null;
        try {
            if (selectTexts == null) {
                selectTexts = connection.prepareStatement("select id from interedition_text order by id desc");
            }
            final ResultSet rs = (resultSet = selectTexts.executeQuery());
            return browser.text(new AbstractIterator<Long>() {
                @Override
                protected Long computeNext() {
                    try {
                        if (rs.next()) {
                            return rs.getLong(1);
                        }
                        return endOfData();
                    } catch (SQLException e) {
                        throw Throwables.propagate(e);
                    }
                }
            });
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        } finally {
            Database.closeQuietly(resultSet);
        }
    }

    public void read(long id, Writer target) {
        read(id, null, target);
    }

    public void read(long id, TextReader consumer) {
        read(id, null, consumer);
    }

    public void read(long id, final Segment segment, final TextReader consumer) {
        withTextContent(id, new TextContentCallback<Void>() {
            @Override
            public Void withContent(Clob text) throws IOException, SQLException {
                Reader content = text.getCharacterStream();
                if (segment != null) {
                    content = new Segment.Reader(content, segment);
                }
                try {
                    consumer.text(content);
                } finally {
                    Closeables.close(content, false);
                }
                return null;
            }
        });
    }

    public void read(long id, final Segment segment, final Writer target) {
        read(id, segment, new TextReader() {
            @Override
            public void text(Reader text) throws IOException {
                CharStreams.copy(text, target);
            }
        });
    }

    public String read(long id) {
        return read(id, (Segment) null);
    }

    public String read(long id, Segment segment) {
        final StringWriter buf = new StringWriter();
        read(id, segment, buf);
        return buf.toString();
    }

    public SortedMap<Segment, String> read(long id, final SortedSet<Segment> segments) {
        return withTextContent(id, new TextContentCallback<SortedMap<Segment, String>>() {
            @Override
            public SortedMap<Segment, String> withContent(Clob text) throws SQLException {
                final SortedMap<Segment, String> result = Maps.newTreeMap();
                for (Segment segment : segments) {
                    result.put(segment, text.getSubString(segment.start() + 1, segment.length()));
                }
                return result;
            }
        });
    }

    public long length(long id) {
        return withTextContent(id, new TextContentCallback<Long>() {
            @Override
            public Long withContent(Clob text) throws IOException, SQLException {
                return text.length();
            }
        });
    }

    public <R> R write(long id, TextWriter<R> writer) {

        try {
            if (insertText == null) {
                insertText = connection.prepareStatement("insert into interedition_text (text_content, id) values (?, ?)");
            }
            final Clob textClob = connection.createClob();
            R result = null;
            Writer textWriter = null;
            try {
                result = writer.write(textWriter = textClob.setCharacterStream(1));
            } finally {
                Closeables.close(textWriter, false);
            }
            insertText.setClob(1, textClob);
            insertText.setLong(2, id);
            insertText.executeUpdate();

            textClob.free();

            addedTexts.add(id);
            removedTexts.remove(id);
            return result;
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public XMLStreamReader write(long id, XMLInputFactory xif, XMLStreamReader reader, TextExtractor extractor, StreamFilter... filters) throws IOException, XMLStreamException {
        return write(id, xif, reader, extractor, Arrays.asList(filters));
    }

    public XMLStreamReader write(long id, final XMLInputFactory xif, final XMLStreamReader reader, final TextExtractor extractor, final Iterable<StreamFilter> filters) throws IOException, XMLStreamException {
        try {
            return write(id, new TextWriter<XMLStreamReader>() {
                @Override
                public XMLStreamReader write(final Writer writer) throws IOException {
                    try {
                        return extractor.execute(xif, reader, Iterables.concat(filters, Collections.singleton(
                                new TextExtractorComponent() {
                                    @Override
                                    protected int text(String text) {
                                        try {
                                            writer.write(text);
                                            return text.length();
                                        } catch (IOException e) {
                                            throw Throwables.propagate(e);
                                        }
                                    }
                                }
                        )));
                    } catch (Throwable t) {
                        Throwables.propagateIfInstanceOf(Throwables.getRootCause(t), IOException.class);
                        throw Throwables.propagate(t);
                    }
                }
            });
        } catch (Throwable t) {
            Throwables.propagateIfInstanceOf(Throwables.getRootCause(t), XMLStreamException.class);
            throw Throwables.propagate(t);
        }
    }

    public void delete(Iterable<Long> ids) {
        if (Iterables.isEmpty(ids)) {
            return;
        }
        try {
            final Object[] idArray = Iterables.toArray(ids, Object.class);

            if (deleteTexts == null) {
                deleteTexts = connection.prepareStatement("delete from interedition_text where id in (?)");
            }
            deleteTexts.setObject(1, idArray);
            deleteTexts.executeUpdate();

            Iterables.removeAll(addedTexts, Arrays.asList(idArray));
            Iterables.addAll(removedTexts, ids);
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        }
    }

    public <R> R annotations(long text, AnnotationReader<R> reader) {
        return annotations(text, null, reader);
    }

    public <R> R annotations(long text, Segment segment, AnnotationReader<R> reader) {
        ResultSet resultSet = null;
        try {
            if (selectAnnotations == null) {
                selectAnnotations = connection.prepareStatement("select " +
                        "a.id, a.anno_data, at.text_id, at.range_start, at.range_end " +
                        "from interedition_text_annotation a " +
                        "join interedition_text_annotation_target q on a.id = q.annotation_id " +
                        "left join interedition_text_annotation_target at on a.id = at.annotation_id " +
                        "where q.text_id = ? and q.range_end > ? and q.range_start < ? " +
                        "order by a.id asc, at.text_id asc, at.range_start asc, at.range_end desc"
                );
            }
            selectAnnotations.setLong(1, text);
            selectAnnotations.setLong(2, segment == null ? 0 : segment.start());
            selectAnnotations.setLong(3, segment == null ? Integer.MAX_VALUE : segment.end());

            final ResultSet rs = resultSet = selectAnnotations.executeQuery();
            final ObjectReader objectReader = objectMapper.reader();
            return reader.read(new AbstractIterator<Annotation>() {

                Annotation current = null;
                SortedSet<AnnotationTarget> currentTargets = null;

                @Override
                protected Annotation computeNext() {
                    try {
                        Annotation next = null;
                        while (rs.next()) {
                            final long id = rs.getLong(1);
                            if (current == null || current.id() != id) {
                                next = current;
                                final Clob data = rs.getClob(2);
                                Reader dataReader = null;
                                try {
                                    current = new Annotation(id,
                                            currentTargets = new TreeSet<AnnotationTarget>(),
                                            objectReader.readTree(dataReader = data.getCharacterStream())
                                    );
                                } finally {
                                    Closeables.close(dataReader, false);
                                }
                                data.free();
                            }
                            final long targetText = rs.getLong(3);
                            if (targetText != 0) {
                                currentTargets.add(new AnnotationTarget(targetText, rs.getInt(4), rs.getInt(5)));
                            }
                            if (next != null) {
                                return next;
                            }
                        }
                        if (current != null) {
                            next = current;
                            current = null;
                        }
                        return (next == null ? endOfData() : next);
                    } catch (IOException e) {
                        throw Throwables.propagate(e);
                    } catch (SQLException e) {
                        throw Throwables.propagate(e);
                    }
                }
            });
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        } finally {
            Database.closeQuietly(resultSet);
        }
    }

    public void annotate(Iterable<Annotation> annotations) {
        if (Iterables.isEmpty(annotations)) {
            return;
        }

        try {
            if (insertAnnotation == null) {
                insertAnnotation = connection.prepareStatement("insert into interedition_text_annotation (id, anno_data) values (?, ?)");
            }
            if (insertTarget == null) {
                insertTarget = connection.prepareStatement("insert into interedition_text_annotation_target (annotation_id, text_id, range_start, range_end) values (?, ?, ?, ?)");
            }

            final ObjectWriter writer = objectMapper.writer();
            for (Annotation annotation : annotations) {

                final long id = annotation.id();
                insertAnnotation.setLong(1, id);
                final Clob dataClob = connection.createClob();
                Writer dataWriter = null;
                try {
                    writer.writeValue(dataWriter = dataClob.setCharacterStream(1), annotation.data());
                } finally {
                    Closeables.close(dataWriter, false);
                }
                insertAnnotation.setClob(2, dataClob);
                insertAnnotation.addBatch();

                dataClob.free();

                for (AnnotationTarget target : annotation.targets()) {
                    insertTarget.setLong(1, id);
                    insertTarget.setLong(2, target.text());
                    insertTarget.setLong(3, target.start());
                    insertTarget.setLong(4, target.end());
                    insertTarget.addBatch();
                }

                addedAnnotations.add(id);
            }
            insertAnnotation.executeBatch();
            insertTarget.executeBatch();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        }
    }


    public Store withSchema() {
        Statement stmt = null;
        try {
            stmt = connection.createStatement();
            stmt.executeUpdate("create table if not exists interedition_text (id bigint primary key, text_content clob not null)");
            stmt.executeUpdate("create table if not exists interedition_text_annotation (id bigint primary key, anno_data clob not null)");
            stmt.executeUpdate("create table if not exists interedition_text_annotation_target (annotation_id bigint not null references interedition_text_annotation (id) on delete cascade, text_id bigint not null, range_start bigint not null, range_end bigint not null)");
            stmt.executeUpdate("create index if not exists interedition_text_annotation_target_text on interedition_text_annotation_target (text_id)");
            stmt.executeUpdate("create index if not exists interedition_text_annotation_target_range on interedition_text_annotation_target (range_start, range_end)");
            return this;
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        } finally {
            Database.closeQuietly(stmt);
        }
    }

    public void backup(Writer to) {
        PreparedStatement script = null;
        ResultSet resultSet = null;
        try {
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
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        } finally {
            Database.closeQuietly(resultSet);
            Database.closeQuietly(script);
        }
    }

    public void restore(File from, Charset charset) {
        Statement runScript = null;
        ResultSet resultSet = null;
        try {
            runScript = connection.createStatement();
            runScript.executeUpdate(String.format("RUNSCRIPT FROM '%s' CHARSET '%s'", from.getPath(), charset.name()));
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        } finally {
            Database.closeQuietly(resultSet);
            Database.closeQuietly(runScript);
        }
    }

    public void restore(Reader from) {
        try {
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
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private interface TextContentCallback<R> {
        R withContent(Clob text) throws IOException, SQLException;
    }

    private <R> R withTextContent(long id, TextContentCallback<R> callback) {
        PreparedStatement query = null;
        ResultSet resultSet = null;
        try {
            query = connection.prepareStatement("SELECT t.text_content FROM interedition_text t WHERE t.id = ?");
            query.setLong(1, id);
            resultSet = query.executeQuery();
            Preconditions.checkArgument(resultSet.next(), id);
            final Clob clob = resultSet.getClob(1);
            final R result = callback.withContent(clob);
            clob.free();
            return result;
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        } finally {
            Database.closeQuietly(resultSet);
            Database.closeQuietly(query);
        }
    }

}
