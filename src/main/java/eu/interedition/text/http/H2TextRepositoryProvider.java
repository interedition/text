package eu.interedition.text.http;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import eu.interedition.text.h2.H2TextRepository;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import org.codehaus.jackson.JsonNode;
import org.h2.jdbcx.JdbcConnectionPool;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class H2TextRepositoryProvider implements Provider<H2TextRepository<JsonNode>> {
    private final File repositoryDirectory;

    @Inject
    public H2TextRepositoryProvider(@Named("interedition.data") String dataDirectory) {
        this.repositoryDirectory = new File(dataDirectory, "texts");
    }

    @Override
    public H2TextRepository<JsonNode> get() {
        final URI dbUri;
        try {
            if (!repositoryDirectory.isDirectory()) {
                repositoryDirectory.mkdirs();
            }
            Preconditions.checkState(repositoryDirectory.isDirectory(), repositoryDirectory.getPath());
            dbUri = new URI("jdbc:h2", repositoryDirectory.getPath(), ";DB_CLOSE_DELAY=-1" +
                    (System.getProperty("interedition.debug") == null ? "" : ";TRACE_LEVEL_SYSTEM_OUT=2"));

            final JdbcConnectionPool dataSource = JdbcConnectionPool.create(dbUri.toString(), "sa", "");
            return new H2TextRepository<JsonNode>(JsonNode.class, dataSource).withSchema();
        } catch (URISyntaxException e) {
            throw Throwables.propagate(e);
        }
    }
}
