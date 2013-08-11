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

package eu.interedition.text.repository;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;
import com.google.common.io.Closer;
import eu.interedition.text.Annotation;
import eu.interedition.text.AnnotationTarget;
import eu.interedition.text.Segment;
import eu.interedition.text.util.Database;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;
import org.codehaus.jackson.map.ObjectWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class JdbcStore implements Store {

    private final Connection connection;
    private final ObjectMapper objectMapper;

    private PreparedStatement selectTexts;
    private PreparedStatement selectAnnotations;
    private PreparedStatement selectTextAnnotations;
    private PreparedStatement selectAnnotationsByTexts;
    private PreparedStatement insertText;
    private PreparedStatement insertAnnotation;
    private PreparedStatement insertTarget;
    private PreparedStatement deleteTexts;
    private PreparedStatement deleteAnnotations;

    private final TransactionLog txLog = new TransactionLog();

    public JdbcStore(Connection connection, ObjectMapper objectMapper) {
        this.connection = connection;
        this.objectMapper = objectMapper;
    }

    public TransactionLog txLog() {
        return txLog;
    }

    public void close() {
        Database.closeQuietly(deleteAnnotations);
        Database.closeQuietly(deleteTexts);
        Database.closeQuietly(insertTarget);
        Database.closeQuietly(insertAnnotation);
        Database.closeQuietly(insertText);
        Database.closeQuietly(selectTextAnnotations);
        Database.closeQuietly(selectAnnotationsByTexts);
        Database.closeQuietly(selectAnnotations);
        Database.closeQuietly(selectTexts);
    }

    public ObjectMapper objectMapper() {
        return objectMapper;
    }

    @Override
    public <R> R contents(ContentsCallback<R> cb) {
        ResultSet resultSet = null;
        try {
            if (selectTexts == null) {
                selectTexts = connection.prepareStatement("select id from interedition_text order by id desc");
            }
            final ResultSet rs = (resultSet = selectTexts.executeQuery());
            return cb.contents(new AbstractIterator<Long>() {
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

    @Override
    public void text(long id, TextCallback cb) {
        text(id, null, cb);
    }

    @Override
    public void text(long id, final Segment segment, final TextCallback cb) {
        withTextContent(id, new TextContentCallback<Void>() {
            @Override
            public Void withContent(Clob text) throws IOException, SQLException {
                Reader content = text.getCharacterStream();
                if (segment != null) {
                    content = new Segment.Reader(content, segment);
                }
                try {
                    cb.text(content);
                } finally {
                    Closeables.close(content, false);
                }
                return null;
            }
        });
    }

    @Override
    public SortedMap<Segment, String> segments(long id, final SortedSet<Segment> segments) {
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

    @Override
    public long textLength(long id) {
        return withTextContent(id, new TextContentCallback<Long>() {
            @Override
            public Long withContent(Clob text) throws IOException, SQLException {
                return text.length();
            }
        });
    }

    public <R> R add(long id, TextWriter<R> writer) {

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

            txLog.textsAdded(id);
            return result;
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void deleteTexts(Iterable<Long> ids) {
        if (Iterables.isEmpty(ids)) {
            return;
        }
        ResultSet rs = null;
        try {
            final Long[] idArray = Iterables.toArray(ids, Long.class);
            final List<Long> annotationIds = Lists.newLinkedList();

            if (selectAnnotationsByTexts == null) {
                selectAnnotationsByTexts = connection.prepareStatement("select distinct at.annotation_id " +
                        "from table(id bigint = ?) text " +
                        "join interedition_text_annotation_target at on text.id = at.text_id");
            }
            selectAnnotationsByTexts.setObject(1, idArray);
            rs = selectAnnotationsByTexts.executeQuery();
            while (rs.next()) {
                annotationIds.add(rs.getLong(1));
            }

            deleteAnnotations(annotationIds);

            if (deleteTexts == null) {
                deleteTexts = connection.prepareStatement("delete from interedition_text where id in " +
                        "(select id from table(id bigint = ?) text)");
            }
            deleteTexts.setObject(1, idArray);
            deleteTexts.executeUpdate();

            txLog.textsRemoved(idArray);
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        }  finally {
            Database.closeQuietly(rs);
        }
    }

    @Override
    public void deleteAnnotations(Iterable<Long> ids) {
        if (Iterables.isEmpty(ids)) {
            return;
        }

        try {
            final Long[] idArray = Iterables.toArray(ids, Long.class);
            if (deleteAnnotations == null) {
                deleteAnnotations = connection.prepareStatement("delete from interedition_text_annotation a where a.id in " +
                        "(select id from table(id bigint = ?) ids)");
            }
            deleteAnnotations.setObject(1, idArray);
            deleteAnnotations.executeUpdate();

            txLog.annotationsRemoved(idArray);
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        }

    }

    @Override
    public <R> R annotations(AnnotationsCallback<R> cb, Iterable<Long> ids) {
        try {
            if (selectAnnotations == null) {
                selectAnnotations = connection.prepareStatement("select " +
                        "a.id, a.anno_data, at.text_id, at.range_start, at.range_end " +
                        "from table(id bigint = ?) ai " +
                        "join interedition_text_annotation a on ai.id = a.id " +
                        "left join interedition_text_annotation_target at on a.id = at.annotation_id " +
                        "order by a.id asc, at.text_id asc, at.range_start asc, at.range_end desc"
                );
            }

            selectAnnotations.setObject(1, Iterables.toArray(ids, Long.class));

            return withAnnotationQuery(selectAnnotations, cb);
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public <R> R annotations(AnnotationsCallback<R> cb, Long... ids) {
        return annotations(cb, Arrays.asList(ids));
    }


    @Override
    public <R> R textAnnotations(long text, AnnotationsCallback<R> cb) {
        return textAnnotations(text, null, cb);
    }

    @Override
    public <R> R textAnnotations(long text, Segment segment, AnnotationsCallback<R> cb) {
        try {
            if (selectTextAnnotations == null) {
                selectTextAnnotations = connection.prepareStatement("select " +
                        "a.id, a.anno_data, at.text_id, at.range_start, at.range_end " +
                        "from interedition_text_annotation a " +
                        "join interedition_text_annotation_target q on a.id = q.annotation_id " +
                        "left join interedition_text_annotation_target at on a.id = at.annotation_id " +
                        "where q.text_id = ? and q.range_end > ? and q.range_start < ? " +
                        "order by a.id asc, at.text_id asc, at.range_start asc, at.range_end desc"
                );
            }
            selectTextAnnotations.setLong(1, text);
            selectTextAnnotations.setLong(2, segment == null ? 0 : segment.start());
            selectTextAnnotations.setLong(3, segment == null ? Integer.MAX_VALUE : segment.end());

            return withAnnotationQuery(selectTextAnnotations, cb);
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        }
    }

    protected <R> R withAnnotationQuery(PreparedStatement query, AnnotationsCallback<R> cb) {
        ResultSet resultSet = null;
        try {
            final ResultSet rs = resultSet = query.executeQuery();
            final ObjectReader objectReader = objectMapper.reader();
            return cb.annotations(new AbstractIterator<Annotation>() {

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

    @Override
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

                txLog.annotationsAdded(id);
            }
            insertAnnotation.executeBatch();
            insertTarget.executeBatch();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        }
    }


    JdbcStore writeSchema() {
        final Closer closer = Closer.create();
        try {
            restore(closer.register(new InputStreamReader(
                    getClass().getResourceAsStream("schema.sql"),
                    Charset.forName("UTF-8")
            )));
            return this;
        } finally {
            try {
                closer.close();
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
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
