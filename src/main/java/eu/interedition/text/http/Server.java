package eu.interedition.text.http;

import com.google.common.collect.Sets;
import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
import com.sun.jersey.api.core.DefaultResourceConfig;
import java.io.IOException;
import java.net.URI;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.grizzly.http.server.HttpServer;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class Server extends DefaultResourceConfig {
    private static final Logger LOG = Logger.getLogger(Server.class.getName());


    public static void main(String... args) throws IOException {
        final URI context = UriBuilder.fromUri("http://localhost/").port(8080).path("/").build();

        if (LOG.isLoggable(Level.INFO)) {
            LOG.info("Starting HTTP server at " + context.toString());
        }

        final HttpServer httpServer = GrizzlyServerFactory.createHttpServer(context, new Server());

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                if (LOG.isLoggable(Level.INFO)) {
                    LOG.info("Stopping HTTP server");
                }
                httpServer.stop();
            }
        }));

        httpServer.start();

        try {
            synchronized (httpServer) {
                httpServer.wait();
            }
        } catch (InterruptedException e) {
        }
    }

    @Override
    public Set<Class<?>> getClasses() {
        return Sets.<Class<?>>newHashSet(LayerResource.class);
    }
}
