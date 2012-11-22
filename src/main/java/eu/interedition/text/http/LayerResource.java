package eu.interedition.text.http;

import javax.ws.rs.Path;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import com.google.inject.Inject;

import eu.interedition.text.h2.H2TextRepository;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor
 *         Middell</a>
 */
@Path("/layer")
public class LayerResource {

	private final H2TextRepository<JsonNode> textRepository;
	private final ObjectMapper objectMapper;
	
	
	@Inject
	public LayerResource(H2TextRepository<JsonNode> textRepository, ObjectMapper objectMapper) {
		this.textRepository = textRepository;
		this.objectMapper = objectMapper;
	}
}
