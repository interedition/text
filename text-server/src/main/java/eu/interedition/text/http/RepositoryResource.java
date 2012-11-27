package eu.interedition.text.http;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import eu.interedition.text.Anchor;
import eu.interedition.text.Layer;
import eu.interedition.text.Name;
import eu.interedition.text.QueryResult;
import eu.interedition.text.Text;
import eu.interedition.text.TextRange;
import eu.interedition.text.TextRepository;
import eu.interedition.text.lisp.LispParserException;
import eu.interedition.text.lisp.QueryParser;
import freemarker.template.Configuration;
import freemarker.template.Template;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

@Path("/")
public class RepositoryResource {

	private TextRepository<JsonNode> repository;
	private final ObjectMapper objectMapper;
    private final Configuration templates;
    private QueryParser<JsonNode> queryParser;

    @Inject
	public RepositoryResource(TextRepository<JsonNode> repository, ObjectMapper objectMapper, Configuration templates) {
		this.repository = repository;
		this.objectMapper = objectMapper;
        this.templates = templates;
        this.queryParser = new QueryParser<JsonNode>(repository);
    }

	@GET
	@Produces(MediaType.TEXT_HTML)
    public Template api() throws IOException {
        return templates.getTemplate("api.ftl");
	}

    @GET
	@Produces(MediaType.APPLICATION_JSON)
    public QueryResult<JsonNode> query(@QueryParam("q") String query) throws IOException, LispParserException {
        return repository.query(queryParser.parse(query));
    }

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response create(ObjectNode data, @Context UriInfo uriInfo) throws IOException {
        final JsonNode nameNode = Preconditions.checkNotNull(data.remove("name"), "No name given");
        final JsonNode anchorsNode = data.remove("anchors");
        final JsonNode textNode = data.remove("text");

        final Name name = objectMapper.readValue(nameNode, Name.class);
        final StringReader textReader = new StringReader(textNode == null ? "" : textNode.asText());
        final Set<Anchor> anchors = Sets.newHashSet();
        for (JsonNode anchorNode : Objects.firstNonNull(anchorsNode, Collections.<JsonNode>emptySet())) {
            Preconditions.checkArgument(anchorNode.isArray() && anchorNode.size() > 2, anchorNode.toString());
            final Text text = Preconditions.checkNotNull(repository.findByIdentifier(anchorNode.get(0).asLong()), anchorNode.get(0).toString());
            final long rangeStart = anchorNode.get(1).asLong();
            final long rangeEnd = anchorNode.get(2).asLong();
            anchors.add(new Anchor(text, new TextRange(rangeStart, rangeEnd)));
        }

        final Layer<JsonNode> created = repository.add(name, textReader, data, anchors);
        return Response.created(uriInfo.getBaseUriBuilder().path("/" + created.getId()).build()).build();
	}

}
