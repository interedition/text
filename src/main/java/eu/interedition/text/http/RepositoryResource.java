package eu.interedition.text.http;

import java.io.IOException;
import java.io.StringReader;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import com.google.inject.Inject;

import eu.interedition.text.Name;
import eu.interedition.text.Query;
import eu.interedition.text.QueryResult;
import eu.interedition.text.TextConstants;
import eu.interedition.text.TextRepository;
import eu.interedition.text.h2.H2TextRepository;
import eu.interedition.text.h2.LayerRelation;
import eu.interedition.text.lisp.LispParserException;
import eu.interedition.text.lisp.QueryParser;

@Path("/")
public class RepositoryResource {

	private TextRepository<JsonNode> textRepository;
	private final ObjectMapper objectMapper;
	
	@Inject
	public RepositoryResource(H2TextRepository<JsonNode> textRepository, ObjectMapper objectMapper) {
		this.textRepository = textRepository;
		this.objectMapper = objectMapper;
	}
	

	@Path("")
	@GET
	public QueryResult<JsonNode> query(@Context Request request, @QueryParam("q") String q) throws LispParserException, IOException {
		final QueryParser<JsonNode> parser = new QueryParser<JsonNode>(textRepository);
		final Query query = parser.parse(q);
		
		return textRepository.query(query);
		
	}
	
	
	
	
	
	
	//test: curl -i -X GET http://localhost:8080/2049
	@GET
	@Path("{layerId}")
	@Produces({ MediaType.APPLICATION_JSON })
	public JsonNode getLayer(@PathParam("layerId") Long layerId, @PathParam("q") String q) {
		System.out.println(layerId);
		
		LayerRelation<JsonNode> layer = null;
		if(q != null && q.length() > 0){
			//search
		}else{
			layer = (LayerRelation<JsonNode>)this.textRepository.findByIdentifier(layerId);
		}
		
		return layerToObjectNode(layer);
	}

	//test: curl -i -X POST -d '{"name":"base", "text":"mi textooo"}' http://localhost:8080/ -H "Content-Type: application/json"  -H "Accept: application/json"
	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public JsonNode postLayer(JsonNode layerJSON) {
		
		System.out.println(layerJSON.toString());
		
		LayerRelation<JsonNode> layer = null;
		try {
			layer = (LayerRelation<JsonNode>) this.textRepository.add(
					new Name(
							TextConstants.INTEREDITION_NS_URI, layerJSON.get("name").toString()),
					new StringReader(layerJSON.get("text").toString()), null);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return layerToObjectNode(layer);
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
