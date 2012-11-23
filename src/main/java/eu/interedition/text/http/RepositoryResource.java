package eu.interedition.text.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import com.google.common.io.Closeables;
import com.google.inject.Inject;
import com.google.inject.name.Named;

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
	private String documentationPath;
	
	@Inject
	public RepositoryResource(H2TextRepository<JsonNode> textRepository, ObjectMapper objectMapper, @Named("interedition.documentation_path") String documentationPath) {
		this.textRepository = textRepository;
		this.objectMapper = objectMapper;
		this.documentationPath = documentationPath;
	}
	

	public QueryResult<JsonNode> query(String q) throws LispParserException, IOException {
		final QueryParser<JsonNode> parser = new QueryParser<JsonNode>(textRepository);
		final Query query = parser.parse(q);
		
		return textRepository.query(query);
		
	}
	
    @GET
    @Path("/")
    public Response stream(@Context Request request) throws IOException {
        InputStream stream = getClass().getResourceAsStream(this.documentationPath);
        

        if (request.getMethod().equals("GET")) {
            final Response.ResponseBuilder preconditions = request.evaluatePreconditions();
            if (preconditions != null) {
                Closeables.close(stream, false);
                throw new WebApplicationException(preconditions.build());
            }
        }
        return Response.ok()
                .entity(stream)
                .build();

    }
    
  //test: curl -H "Accept: application/json" -i -X GET http://localhost:8080/2049?q=asdas
    @GET
	@Path("{layerId}")
	@Produces({ MediaType.APPLICATION_JSON })
	public Object queryLayer(@PathParam("layerId") Long layerId, @QueryParam("q") String q) throws LispParserException, IOException {
    	QueryResult<JsonNode> rs = query(q);
		return rs;
    }
    
	
	//test: curl -H "Accept: application/json" -i -X GET http://localhost:8080/2049
	@GET
	@Path("{layerId}")
	@Produces({ MediaType.APPLICATION_JSON })
	public LayerRelation<JsonNode> getLayer(@PathParam("layerId") Long layerId) throws LispParserException, IOException {
		LayerRelation<JsonNode> layer = (LayerRelation<JsonNode>)this.textRepository.findByIdentifier(layerId);
		return layer;
	}
	
	//curl -H "Accept: text/plain" http://localhost:8080/2049
	@GET
	@Path("{layerId}")
	@Produces({ MediaType.TEXT_PLAIN })
	public String getLayerText(@PathParam("layerId") Long layerId) throws LispParserException, IOException {
		System.out.println(layerId);
		
		LayerRelation<JsonNode> layer = (LayerRelation<JsonNode>)this.textRepository.findByIdentifier(layerId);
		if(layer != null){
			return layer.read();
		}
		return null;
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
