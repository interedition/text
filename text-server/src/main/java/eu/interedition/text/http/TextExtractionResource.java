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
import com.google.common.collect.Sets;
import com.sun.jersey.multipart.FormDataParam;
import eu.interedition.text.Repository;
import eu.interedition.text.lmnl.ClixRangeAnnotationGenerator;
import eu.interedition.text.http.io.Templates;
import eu.interedition.text.Texts;
import eu.interedition.text.tei.MilestoneAnnotationGenerator;
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

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
@Path("/xml-extract")
@Singleton
public class TextExtractionResource {

    final Repository repository;
    final Templates templates;
    final XMLInputFactory2 xmlInputFactory;
    final ConverterBuilder converterBuilder;

    @Inject
    public TextExtractionResource(Repository repository, ObjectMapper objectMapper, Templates templates) {
        this.repository = repository;
        this.templates = templates;
        this.xmlInputFactory = XML.createXMLInputFactory();
        this.converterBuilder = Converter.builder()
                .withNamespaceMapping(new NamespaceMapping())
                .withNodePath()
                .withOffsetMapping()
                .withWhitespaceCompression(new WhitespaceStrippingContext.ElementNameBased(
                        Sets.newHashSet("div", "text", "body")
                ))
                .filter(new LineBreaker.ElementNamedBased(Sets.newHashSet("p", "div", "l", "lg", "sp", "speaker")))
                .filter(new FilterContext.ElementNameBased(
                        Sets.newHashSet("text"),
                        Sets.newHashSet("TEI", "TEI.2", "back", "front", "note", "figure", "fw")
                ))
                .filter(new ClixRangeAnnotationGenerator(objectMapper))
                .filter(new MilestoneAnnotationGenerator(objectMapper))
                .filter(new AnnotationGenerator.Elements(objectMapper))
                .filter(new TextGenerator());
    }

    @GET
    public Response html() throws IOException {
        return templates.render("xml-extract.ftl");
    }

    @POST
    @Consumes(MediaType.APPLICATION_XML)
    public Response rest(@Context UriInfo uriInfo, Source xml) throws XMLStreamException, IOException, TransformerException {
        return Response.created(extract(uriInfo, xml)).build();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response form(@Context UriInfo uriInfo, @FormDataParam("xml") InputStream xml) throws TransformerException, XMLStreamException, IOException {
        return Response.seeOther(extract(uriInfo, new StreamSource(xml))).build();
    }

    URI extract(UriInfo uriInfo, Source source) throws XMLStreamException, IOException {
        XMLStreamReader reader = null;
        try {
            final XMLStreamReader xml = reader = xmlInputFactory.createXMLStreamReader(source);
            final long id = Texts.xml(converterBuilder, repository, xmlInputFactory, xml);
            return uriInfo.getBaseUriBuilder().path("/text/" + id).build();
        } catch (Throwable t) {
            final Throwable rootCause = Throwables.getRootCause(t);
            Throwables.propagateIfInstanceOf(rootCause, XMLStreamException.class);
            Throwables.propagateIfInstanceOf(rootCause, IOException.class);
            throw Throwables.propagate(t);
        } finally {
            XML.closeQuietly(reader);
        }
    }
}
