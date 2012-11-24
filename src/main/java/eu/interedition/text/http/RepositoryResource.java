package eu.interedition.text.http;

import com.google.inject.Inject;
import eu.interedition.text.Name;
import eu.interedition.text.Query;
import eu.interedition.text.QueryResult;
import eu.interedition.text.TextRepository;
import eu.interedition.text.h2.H2TextRepository;
import eu.interedition.text.h2.LayerRelation;
import eu.interedition.text.lisp.LispParserException;
import eu.interedition.text.lisp.QueryParser;
import freemarker.template.Configuration;
import freemarker.template.Template;
import java.io.IOException;
import java.io.StringReader;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

@Path("/")
public class RepositoryResource {

	private TextRepository<JsonNode> textRepository;
	private final ObjectMapper objectMapper;
    private final Configuration templates;

    @Inject
	public RepositoryResource(H2TextRepository<JsonNode> textRepository, ObjectMapper objectMapper, Configuration templates) {
		this.textRepository = textRepository;
		this.objectMapper = objectMapper;
        this.templates = templates;
    }

	public QueryResult<JsonNode> query(String q) throws LispParserException, IOException {
		final QueryParser<JsonNode> parser = new QueryParser<JsonNode>(textRepository);
		final Query query = parser.parse(q);
		return textRepository.query(query);
		
	}

	@GET
	@Produces(MediaType.TEXT_HTML)
    public Template api() throws IOException {
        return templates.getTemplate("api.ftl");
	}
	
    @GET
	@Produces({ MediaType.APPLICATION_JSON })
    public QueryResult<JsonNode> queryRepository(@Context Request request, @QueryParam("q") String q) throws IOException, LispParserException {        	
        	QueryResult<JsonNode> rs = query(q);
        	System.out.println(rs);
        	return rs;    						    		
    }	

	//test: curl -i -X POST -d '{"name":["http://interedition.eu/ns","base"], "text":"my text"}'
	//http://localhost:8080/ -H "Content-Type: application/json"  -H "Accept: application/json"
	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public Object postLayer(JsonNode layerJSON) {
		
		System.out.println(layerJSON.toString());
		
		LayerRelation<JsonNode> layer = null;
		try {
			if(layerJSON.get("anchors") == null){
				layer = (LayerRelation<JsonNode>) this.textRepository.add(
						new Name(
								layerJSON.get("name").get(0).toString(), layerJSON.get("name").get(1).toString()),
						new StringReader(layerJSON.get("text").toString()), null);	
			}else{
				//TODO set the anchors (annotation)
			}
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		ObjectNode result = objectMapper.createObjectNode();
		result.put("id", layer.getId());
		return result;
	}
	
	
	private ObjectNode layerToObjectNode(LayerRelation<JsonNode> layer){
		ObjectNode result = objectMapper.createObjectNode();
		if(layer != null){
			result.put("id", layer.getId());
			result.put("name", layer.getName().getLocalName());
			try {
				result.put("text", layer.read());
			} catch (IOException e) {
				e.printStackTrace();
			}
			//TODO put the range in the json object
		}
		return result;
	}

}
