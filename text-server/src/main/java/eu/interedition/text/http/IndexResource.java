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

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import eu.interedition.text.Repository;
import eu.interedition.text.http.io.Templates;
import eu.interedition.text.repository.Store;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
@Path("/")
@Singleton
public class IndexResource {

    private final Repository repository;
    private final Templates templates;

    @Inject
    public IndexResource(Repository repository, Templates templates) {
        this.repository = repository;
        this.templates = templates;
    }

    @GET
    public Response index() throws IOException, SQLException {
        return templates.render("index.ftl", repository.execute(new Repository.Transaction<Map<String, Object>>() {
            @Override
            public Map<String, Object> transactional(Store store) {
                return Collections.<String, Object>singletonMap("texts", store.contents(new Store.ContentsCallback<List<Long>>() {
                    @Override
                    public List<Long> contents(Iterator<Long> ids) {
                        return Lists.newArrayList(Iterators.limit(ids, 1000));
                    }
                }));
            }
        }));
    }
}
