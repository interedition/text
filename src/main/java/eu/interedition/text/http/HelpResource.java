package eu.interedition.text.http;

import com.google.common.base.Strings;
import com.google.common.io.Closeables;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import eu.interedition.text.h2.H2TextRepository;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import org.codehaus.jackson.JsonNode;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor
 * Middell</a>
 */
@Path("/")
public class HelpResource {

    private final H2TextRepository<JsonNode> textRepository;
    private String documentationPath;

    @Inject
    public HelpResource(@Named("interedition.documentation_path") String documentationPath, H2TextRepository<JsonNode> textRepository) {
        this.documentationPath = documentationPath;
        this.textRepository = textRepository;
    }

    @GET
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
}
