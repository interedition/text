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

package eu.interedition.text;

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import eu.interedition.text.repository.SqlRepository;
import eu.interedition.text.repository.Store;
import eu.interedition.text.repository.Stores;
import eu.interedition.text.util.Database;
import eu.interedition.text.xml.AnnotationWriter;
import eu.interedition.text.xml.ContainerElementContext;
import eu.interedition.text.xml.ContextualStreamFilter;
import eu.interedition.text.xml.LineBreaker;
import eu.interedition.text.xml.NamespaceMapping;
import eu.interedition.text.xml.TextExtractor;
import eu.interedition.text.xml.XML;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.stax2.XMLInputFactory2;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.sql.DataSource;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.sql.SQLException;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class XMLTest {

    protected static DataSource dataSource;
    protected static Repository repository;

    @BeforeClass
    public static void init() throws SQLException {
        repository = new SqlRepository(dataSource = Database.h2(), new ObjectMapper()).withSchema();
    }

    @Test
    public void dostoyevsky() throws Exception {
        gutenbergText("/dostoyevsky.xml");
    }

    @Test
    public void iliad() throws Exception {
        gutenbergText("/homer-iliad-tei.xml");
    }

    protected void gutenbergText(final String resource) throws Exception {
        final XMLInputFactory2 xif = XML.createXMLInputFactory();
        XMLStreamReader reader = null;
        try {
            final XMLStreamReader xml = reader = xif.createXMLStreamReader(getClass().getResource(resource));
            reader = repository.execute(new Repository.Transaction<XMLStreamReader>() {
                @Override
                public XMLStreamReader transactional(Store store) {
                    try {
                        final long id = repository.textIds().next();
                        return Stores.xml(store, id, xif, xml,
                                new TextExtractor().withWhitespaceCompression(new ContainerElementContext.ElementNameBased(
                                        Sets.newHashSet("div")
                                )).withNamespaceMapping(new NamespaceMapping()),
                                new LineBreaker.ElementNamedBased(Sets.newHashSet("p", "div", "l", "lg", "sp", "speaker")),
                                new ContextualStreamFilter.ElementNameBased(
                                        Sets.newHashSet("text"),
                                        Sets.newHashSet("TEI.2", "back", "front", "note", "figure")
                                ),
                                new AnnotationWriter.Elements(store, id, repository.annotationIds())
                        );
                    } catch (IOException e) {
                        throw Throwables.propagate(e);
                    } catch (XMLStreamException e) {
                        throw Throwables.propagate(e);
                    }
                }
            });
        } finally {
            XML.closeQuietly(reader);
        }
    }
}
