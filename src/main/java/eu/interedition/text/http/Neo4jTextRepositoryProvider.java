package eu.interedition.text.http;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import eu.interedition.text.json.JacksonDataNodeMapper;
import eu.interedition.text.neo4j.Neo4jTextRepository;
import java.io.File;
import java.io.IOException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.neo4j.kernel.EmbeddedGraphDatabase;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class Neo4jTextRepositoryProvider implements Provider<Neo4jTextRepository<JsonNode>> {
    private final File repositoryDirectory;
    private final ObjectMapper objectMapper;

    @Inject
    public Neo4jTextRepositoryProvider(@Named("interedition.data") String dataDirectory, ObjectMapper objectMapper) {
        this.repositoryDirectory = new File(dataDirectory, "texts");
        this.objectMapper = objectMapper;
    }

    @Override
    public Neo4jTextRepository<JsonNode> get() {
        try {
            if (!repositoryDirectory.isDirectory()) {
                repositoryDirectory.mkdirs();
            }
            Preconditions.checkState(repositoryDirectory.isDirectory(), repositoryDirectory.getPath());

            final EmbeddedGraphDatabase graphDatabase = new EmbeddedGraphDatabase(repositoryDirectory.getCanonicalPath());
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    graphDatabase.shutdown();
                }
            }));

            return new Neo4jTextRepository<JsonNode>(JsonNode.class, new JacksonDataNodeMapper<JsonNode>(objectMapper), graphDatabase, true);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
}
