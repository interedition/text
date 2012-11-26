package eu.interedition.text.http;

import com.google.inject.Inject;
import eu.interedition.text.Layer;
import eu.interedition.text.Query;
import eu.interedition.text.QueryResult;
import eu.interedition.text.TextRepository;
import eu.interedition.text.lisp.LispParserException;
import eu.interedition.text.lisp.QueryParser;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.codehaus.jackson.JsonNode;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor
 *         Middell</a>
 */
@Path("/{id}")
public class LayerResource {

	private final TextRepository<JsonNode> textRepository;
    private final Configuration templates;
    private final QueryParser<JsonNode> queryParser;

    @Inject
	public LayerResource(TextRepository<JsonNode> textRepository, Configuration templates) {
		this.textRepository = textRepository;
        this.templates = templates;
        this.queryParser = new QueryParser<JsonNode>(textRepository);
	}
	
    @GET
	@Produces(MediaType.APPLICATION_JSON)
	public QueryResult<JsonNode> query(@PathParam("id") Long id, @QueryParam("q") String query) throws LispParserException, IOException {
        //test: curl -H "Accept: application/json" -i -X GET http://localhost:8080/2049
        //test: curl -H "Accept: application/json" -i -X GET http://localhost:8080/2049?q=asdas
        return textRepository.query(query == null ? Query.is(id) : queryParser.parse(query));
    }
	
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String text(@PathParam("id") Long id) throws LispParserException, IOException {
        //curl -H "Accept: text/plain" http://localhost:8080/2049
        final Layer<JsonNode> layer = this.textRepository.findByIdentifier(id);
        return (layer == null ? null : layer.read());
	}
	
	@GET
	@Produces(MediaType.TEXT_HTML)
	public String html(@PathParam("id") Long id) throws LispParserException, IOException, TemplateException {
        //curl -H "Accept: text/html" http://localhost:8080/2049
        final Layer<JsonNode> layer = this.textRepository.findByIdentifier(id);
        if (layer == null) {
            return null;
        }
        final StringWriter html = new StringWriter();
        templates.getTemplate("layer.ftl").process(Collections.singletonMap("text", layer.read()), html);
        return html.toString();
	}

}
