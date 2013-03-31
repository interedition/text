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
import com.google.common.collect.Sets;
import com.sun.jersey.multipart.FormDataParam;
import eu.interedition.text.http.io.Templates;
import eu.interedition.text.ld.IdentifierGenerator;
import eu.interedition.text.ld.Store;
import eu.interedition.text.ld.Transactions;
import eu.interedition.text.ld.clix.ClixRangeAnnotationWriter;
import eu.interedition.text.ld.tei.MilestoneAnnotationWriter;
import eu.interedition.text.ld.xml.AnnotationWriter;
import eu.interedition.text.ld.xml.ContainerElementContext;
import eu.interedition.text.ld.xml.ContextualStreamFilter;
import eu.interedition.text.ld.xml.LineBreaker;
import eu.interedition.text.ld.xml.NamespaceMapping;
import eu.interedition.text.ld.xml.TextExtractor;
import eu.interedition.text.ld.xml.XML;
import org.codehaus.stax2.XMLInputFactory2;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.sql.SQLException;
import java.util.List;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
@Path("/xml-extract")
public class TextExtractionResource {

    final Transactions transactions;
    final IdentifierGenerator textIds;
    final IdentifierGenerator annotationIds;
    final Templates templates;
    final XMLInputFactory2 xmlInputFactory;

    @Inject
    public TextExtractionResource(Transactions transactions,
                                  @Named("texts") IdentifierGenerator textIds,
                                  @Named("annotations") IdentifierGenerator annotationIds,
                                  Templates templates) {
        this.transactions = transactions;
        this.textIds = textIds;
        this.annotationIds = annotationIds;
        this.templates = templates;
        this.xmlInputFactory = XML.createXMLInputFactory();
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
            final long id = textIds.next();
            transactions.execute(new Transactions.Transaction<Object>() {
                @Override
                public Object withStore(Store store) throws SQLException {
                    try {
                        store.write(id, xmlInputFactory, xml, createTextExtractor(), createTextExtractionFilterChain(store, id));
                        return null;
                    } catch (IOException e) {
                        throw Throwables.propagate(e);
                    } catch (XMLStreamException e) {
                        throw Throwables.propagate(e);
                    }
                }
            });
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

    TextExtractor createTextExtractor() {
        return new TextExtractor()
                .withNamespaceMapping(new NamespaceMapping())
                .withWhitespaceCompression(new ContainerElementContext.ElementNameBased(
                        Sets.newHashSet("div", "text", "body")
                ));
    }

    List<StreamFilter> createTextExtractionFilterChain(Store store, long text) {
        return Lists.<StreamFilter>newArrayList(
                new LineBreaker.ElementNamedBased(Sets.newHashSet("p", "div", "l", "lg", "sp", "speaker")),
                new ContextualStreamFilter.ElementNameBased(
                        Sets.newHashSet("text"),
                        Sets.newHashSet("TEI", "TEI.2", "back", "front", "note", "figure", "fw")
                ),
                new ClixRangeAnnotationWriter(store, text, annotationIds),
                new MilestoneAnnotationWriter(store, text, annotationIds),
                new AnnotationWriter.Elements(store, text, annotationIds)
        );
    }
}
