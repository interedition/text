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

import com.google.common.collect.Sets;
import eu.interedition.text.repository.JdbcRepository;
import eu.interedition.text.util.Database;
import eu.interedition.text.xml.AnnotationGenerator;
import eu.interedition.text.xml.FilterContext;
import eu.interedition.text.xml.WhitespaceStrippingContext;
import eu.interedition.text.xml.Converter;
import eu.interedition.text.xml.ConverterBuilder;
import eu.interedition.text.xml.LineBreaker;
import eu.interedition.text.xml.NamespaceMapping;
import eu.interedition.text.xml.TextGenerator;
import eu.interedition.text.xml.XML;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.stax2.XMLInputFactory2;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.sql.DataSource;
import javax.xml.stream.XMLStreamReader;
import java.sql.SQLException;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class XMLTest {

    protected static DataSource dataSource;
    protected static Repository repository;
    protected static ObjectMapper objectMapper;
    protected static ConverterBuilder converterBuilder;

    @BeforeClass
    public static void init() throws SQLException {
        objectMapper = new ObjectMapper();
        repository = new JdbcRepository(dataSource = Database.h2(), objectMapper).withSchema();
        converterBuilder = Converter.builder()
                .withWhitespaceCompression(new WhitespaceStrippingContext.ElementNameBased(Sets.newHashSet("div")))
                .withNamespaceMapping(new NamespaceMapping())
                .filter(new LineBreaker.ElementNamedBased(Sets.newHashSet("p", "div", "l", "lg", "sp", "speaker")))
                .filter(new FilterContext.ElementNameBased(
                        Sets.newHashSet("text"),
                        Sets.newHashSet("TEI.2", "back", "front", "note", "figure")
                ))
                .filter(new AnnotationGenerator.Elements(objectMapper))
                .filter(new TextGenerator());
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
            Texts.xml(converterBuilder, repository, xif, reader = xif.createXMLStreamReader(getClass().getResource(resource)));
        } finally {
            XML.closeQuietly(reader);
        }
    }
}
