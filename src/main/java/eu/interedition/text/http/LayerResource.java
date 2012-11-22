package eu.interedition.text.http;

import com.google.inject.Inject;
import eu.interedition.text.h2.H2TextRepository;
import javax.ws.rs.Path;
import org.codehaus.jackson.JsonNode;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
@Path("/layer")
public class LayerResource {

    private final H2TextRepository<JsonNode> textRepository;

    @Inject
    public LayerResource(H2TextRepository<JsonNode> textRepository) {
        this.textRepository = textRepository;
    }
}
