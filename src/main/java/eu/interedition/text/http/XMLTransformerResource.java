package eu.interedition.text.http;

import com.google.inject.Inject;
import eu.interedition.text.Anchor;
import eu.interedition.text.Layer;
import eu.interedition.text.Name;
import eu.interedition.text.TextConstants;
import eu.interedition.text.h2.H2TextRepository;
import eu.interedition.text.simple.SimpleLayer;
import eu.interedition.text.xml.XMLTransformer;
import eu.interedition.text.xml.XMLTransformerConfigurationBase;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
@Path("/xml-transform")
public class XMLTransformerResource {

    private final H2TextRepository<JsonNode> textRepository;
    private final ObjectMapper objectMapper;

    @Inject
    public XMLTransformerResource(H2TextRepository<JsonNode> textRepository, ObjectMapper objectMapper) {
        this.textRepository = textRepository;
        this.objectMapper = objectMapper;
    }

    @POST
    @Consumes(MediaType.APPLICATION_XML)
    public Response transform(Reader xml, @Context UriInfo uriInfo) throws XMLStreamException, IOException, TransformerException {
        final XMLTransformerConfigurationBase<JsonNode> transformConfig = new XMLTransformerConfigurationBase<JsonNode>(textRepository) {
            @Override
            protected Layer<JsonNode> translate(Name name, Map<Name, Object> attributes, Set<Anchor> anchors) {
                final ObjectNode data = objectMapper.createObjectNode();
                for (Map.Entry<Name, Object> attr : attributes.entrySet()) {
                    data.put(attr.getKey().toString(), attr.getValue().toString());
                }
                return new SimpleLayer<JsonNode>(name, null, data, anchors);
            }
        };

        final Layer<JsonNode> source = textRepository.add(new Name(TextConstants.XML_NS_URI, "document"), xml, null);
        final Layer<JsonNode> result = new XMLTransformer<JsonNode>(transformConfig).transform(source);
        return Response.created(uriInfo.getBaseUriBuilder().path("/" + result.getId()).build()).build();
    }
}
