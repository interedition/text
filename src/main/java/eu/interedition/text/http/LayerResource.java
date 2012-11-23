package eu.interedition.text.http;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import com.google.inject.Inject;

import eu.interedition.text.Query;
import eu.interedition.text.QueryResult;
import eu.interedition.text.h2.H2TextRepository;
import eu.interedition.text.h2.LayerRelation;
import eu.interedition.text.lisp.LispParserException;
import eu.interedition.text.lisp.QueryParser;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor
 *         Middell</a>
 */
@Path("/{layerId}")
public class LayerResource {

	private final H2TextRepository<JsonNode> textRepository;
	private final ObjectMapper objectMapper;	
	
	@Inject
	public LayerResource(H2TextRepository<JsonNode> textRepository, ObjectMapper objectMapper) {
		this.textRepository = textRepository;
		this.objectMapper = objectMapper;
	}
	
	private QueryResult<JsonNode> query(String q) throws LispParserException, IOException {
		final QueryParser<JsonNode> parser = new QueryParser<JsonNode>(textRepository);
		final Query query = parser.parse(q);
		return textRepository.query(query);	
	}
	
	//test: curl -H "Accept: application/json" -i -X GET http://localhost:8080/2049
	//test: curl -H "Accept: application/json" -i -X GET http://localhost:8080/2049?q=asdas
    @GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Object queryLayer(@PathParam("layerId") Long layerId, @QueryParam("q") String q) throws LispParserException, IOException {
    	
    	if (q == null) {
    		LayerRelation<JsonNode> layer = (LayerRelation<JsonNode>)this.textRepository.findByIdentifier(layerId);
    		return layer;    		
    	} else {
        	QueryResult<JsonNode> rs = query(q);
    		return rs;
    	}
    	
    }
	
	//curl -H "Accept: text/plain" http://localhost:8080/2049
	@GET
	@Produces({ MediaType.TEXT_PLAIN })
	public String getLayerText(@PathParam("layerId") Long layerId) throws LispParserException, IOException {
		System.out.println(layerId);
		
		LayerRelation<JsonNode> layer = (LayerRelation<JsonNode>)this.textRepository.findByIdentifier(layerId);
		if(layer != null){
			return layer.read();
		}
		return null;
	}
}
