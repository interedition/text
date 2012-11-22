package eu.interedition.text.http;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import com.google.common.collect.Multiset.Entry;
import com.google.inject.Inject;

import eu.interedition.text.Anchor;
import eu.interedition.text.Layer;
import eu.interedition.text.Name;
import eu.interedition.text.TextConstants;
import eu.interedition.text.h2.H2TextRepository;
import eu.interedition.text.h2.LayerRelation;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.util.JSONPObject;
import org.codehaus.jackson.node.ObjectNode;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor
 *         Middell</a>
 */
@Path("/")
public class LayerResource {

	private final H2TextRepository<JsonNode> textRepository;
	private final ObjectMapper objectMapper;
	
	
	@Inject
	public LayerResource(H2TextRepository<JsonNode> textRepository, ObjectMapper objectMapper) {
		this.textRepository = textRepository;
		this.objectMapper = objectMapper;
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
		}
		return result;
	}
}
