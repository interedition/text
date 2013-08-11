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

package eu.interedition.text.http;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import eu.interedition.text.Repository;
import eu.interedition.text.http.io.Templates;
import eu.interedition.text.Annotation;
import eu.interedition.text.Segment;
import eu.interedition.text.repository.Store;
import eu.interedition.text.Texts;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
@Path("/text/{id}")
@Singleton
public class TextResource {

    private final Repository repository;
    private final Templates templates;
    private final ObjectMapper objectMapper;

    @Inject
    public TextResource(Repository repository, Templates templates, ObjectMapper objectMapper) {
        this.repository = repository;
        this.templates = templates;
        this.objectMapper = objectMapper;
    }

    @Path("/{start}/{end}")
    @GET
    public Response text(@PathParam("id") final long id, @PathParam("start") int start, @PathParam("end") int end) throws SQLException, IOException {
        final Segment segment = new Segment(Math.max(0, start), Math.min(start + 100000, end));
        try {
            return templates.render("text.ftl", repository.execute(new Repository.Transaction<Map<String, Object>>() {
                @Override
                public Map<String, Object> transactional(Store store) {
                    try {
                        final long length = store.textLength(id);
                        final Segment textSegment = new Segment(segment.start(), (int) Math.min(segment.end(), length));
                        final ObjectWriter writer = objectMapper.writer();

                        final Map<String, Object> view = Maps.newHashMap();
                        view.put("id", id);
                        view.put("length", length);
                        view.put("segment", writer.writeValueAsString(textSegment));
                        view.put("text", Texts.toString(store, id, textSegment));
                        view.put("annotations", writer.writeValueAsString(store.textAnnotations(id, textSegment, new Store.AnnotationsCallback<List<Annotation>>() {
                            @Override
                            public List<Annotation> annotations(Iterator<Annotation> annotations) {
                                return Lists.newArrayList(annotations);
                            }
                        })));
                        return view;
                    } catch (IOException e) {
                        throw Throwables.propagate(e);
                    }
                }
            }));
        } catch (Throwable t) {
            Throwables.propagateIfInstanceOf(Throwables.getRootCause(t), IOException.class);
            throw Throwables.propagate(t);
        }
    }

    @GET
    public Response text(@PathParam("id") final long id) throws SQLException, IOException {
        return text(id, 0, Integer.MAX_VALUE);
    }
}
