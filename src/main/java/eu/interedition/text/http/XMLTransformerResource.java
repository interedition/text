package eu.interedition.text.http;

import com.google.common.io.Closeables;
import com.google.common.io.FileBackedOutputStream;
import com.google.inject.Inject;
import com.sun.jersey.multipart.FormDataParam;
import eu.interedition.text.Anchor;
import eu.interedition.text.Layer;
import eu.interedition.text.Name;
import eu.interedition.text.TextConstants;
import eu.interedition.text.h2.H2TextRepository;
import eu.interedition.text.simple.SimpleLayer;
import eu.interedition.text.xml.XMLTransformer;
import eu.interedition.text.xml.XMLTransformerConfigurationBase;
import eu.interedition.text.xml.XMLTransformerModule;
import eu.interedition.text.xml.module.CLIXAnnotationXMLTransformerModule;
import eu.interedition.text.xml.module.DefaultAnnotationXMLTransformerModule;
import eu.interedition.text.xml.module.LineElementXMLTransformerModule;
import eu.interedition.text.xml.module.NotableCharacterXMLTransformerModule;
import eu.interedition.text.xml.module.TEIAwareAnnotationXMLTransformerModule;
import eu.interedition.text.xml.module.TextXMLTransformerModule;
import freemarker.template.Configuration;
import freemarker.template.Template;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import static eu.interedition.text.TextConstants.TEI_NS;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
@Path("/xml-transform")
public class XMLTransformerResource {

    public static final Charset CHARSET = Charset.forName("UTF-8");
    private final H2TextRepository<JsonNode> textRepository;
    private final Configuration templates;
    private final ObjectMapper objectMapper;
    private TransformerFactory transformerFactory;

    @Inject
    public XMLTransformerResource(H2TextRepository<JsonNode> textRepository, Configuration templates, final ObjectMapper objectMapper) {
        this.textRepository = textRepository;
        this.templates = templates;
        this.objectMapper = objectMapper;
        this.transformerFactory = TransformerFactory.newInstance();
    }

    @GET
    @Consumes(MediaType.TEXT_HTML)
    public Template html() throws IOException {
        return templates.getTemplate("xml-transform.ftl");
    }

    @POST
    @Consumes(MediaType.APPLICATION_XML)
    public Response rest(Source xml, @Context UriInfo uriInfo) throws XMLStreamException, IOException, TransformerException {
        return Response.created(uriInfo.getBaseUriBuilder()
                .path("/" + transform(xml).getId())
                .build()).build();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response form(@FormDataParam("xml") InputStream xml, @Context UriInfo uriInfo) throws TransformerException, XMLStreamException, IOException {
        return Response.seeOther(uriInfo.getBaseUriBuilder()
                .path("/" + transform(new StreamSource(xml)).getId())
                .build()).build();
    }

    protected Layer<JsonNode> transform(Source xml) throws TransformerException, IOException, XMLStreamException {
        FileBackedOutputStream xmlBuf = null;
        try {
            final OutputStreamWriter xmlBufWriter = new OutputStreamWriter(xmlBuf = new FileBackedOutputStream(102400, true), CHARSET);
            transformerFactory.newTransformer().transform(xml, new StreamResult(xmlBufWriter));
            xmlBufWriter.flush();
        } finally {
            Closeables.close(xmlBuf, false);
        }

        InputStream xmlBufIn = null;
        try {
            InputStreamReader xmlBufReader = new InputStreamReader(xmlBufIn = xmlBuf.getSupplier().getInput(), CHARSET);
            final Layer<JsonNode> source = textRepository.add(new Name(TextConstants.XML_NS_URI, "document"), xmlBufReader, null);
            return new XMLTransformer<JsonNode>(createXMLTransformerConfig()).transform(source);
        } finally {
            Closeables.close(xmlBufIn, false);
            xmlBuf.reset();
        }
    }

    protected XMLTransformerConfigurationBase<JsonNode> createXMLTransformerConfig() {
        XMLTransformerConfigurationBase<JsonNode> xmlTransformConfig = new XMLTransformerConfigurationBase<JsonNode>(textRepository) {
            @Override
            protected Layer<JsonNode> translate(Name name, Map<Name, Object> attributes, Set<Anchor> anchors) {
                final ObjectNode data = objectMapper.createObjectNode();
                for (Map.Entry<Name, Object> attr : attributes.entrySet()) {
                    data.put(attr.getKey().toString(), attr.getValue().toString());
                }
                return new SimpleLayer<JsonNode>(name, "", data, anchors);
            }
        }.withBatchSize(1024);

        final List<XMLTransformerModule<JsonNode>> modules = xmlTransformConfig.getModules();
        modules.add(new LineElementXMLTransformerModule<JsonNode>());
        modules.add(new NotableCharacterXMLTransformerModule<JsonNode>());
        modules.add(new TextXMLTransformerModule<JsonNode>());
        modules.add(new DefaultAnnotationXMLTransformerModule<JsonNode>());
        modules.add(new CLIXAnnotationXMLTransformerModule<JsonNode>());
        modules.add(new TEIAwareAnnotationXMLTransformerModule<JsonNode>());

        xmlTransformConfig.addLineElement(new Name(TEI_NS, "div"));
        xmlTransformConfig.addLineElement(new Name(TEI_NS, "head"));
        xmlTransformConfig.addLineElement(new Name(TEI_NS, "sp"));
        xmlTransformConfig.addLineElement(new Name(TEI_NS, "stage"));
        xmlTransformConfig.addLineElement(new Name(TEI_NS, "speaker"));
        xmlTransformConfig.addLineElement(new Name(TEI_NS, "lg"));
        xmlTransformConfig.addLineElement(new Name(TEI_NS, "l"));
        xmlTransformConfig.addLineElement(new Name(TEI_NS, "p"));

        xmlTransformConfig.addContainerElement(new Name(TEI_NS, "text"));
        xmlTransformConfig.addContainerElement(new Name(TEI_NS, "div"));
        xmlTransformConfig.addContainerElement(new Name(TEI_NS, "lg"));
        xmlTransformConfig.addContainerElement(new Name(TEI_NS, "subst"));
        xmlTransformConfig.addContainerElement(new Name(TEI_NS, "choice"));

        xmlTransformConfig.exclude(new Name(TEI_NS, "teiHeader"));
        xmlTransformConfig.exclude(new Name(TEI_NS, "front"));
        xmlTransformConfig.exclude(new Name(TEI_NS, "fw"));
        xmlTransformConfig.exclude(new Name(TEI_NS, "app"));

        xmlTransformConfig.include(new Name(TEI_NS, "lem"));

        return xmlTransformConfig;
    }
}
