package eu.interedition.text.http;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.sun.jersey.api.container.filter.GZIPContentEncodingFilter;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.server.impl.container.filter.NormalizeFilter;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import eu.interedition.text.Transactions;
import eu.interedition.text.util.Database;
import eu.interedition.text.util.TextModule;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.codehaus.jackson.map.ObjectMapper;

import javax.sql.DataSource;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
@Module(injects = HttpService.class)
public class Server implements Runnable {
    private static final Logger LOG = Logger.getLogger(Server.class.getName());

    Deque<Service> services = Lists.newLinkedList();
    Configuration configuration;

    public static void main(String... args) {
        try {
            final CommandLine commandLine = new GnuParser().parse(OPTIONS, args);
            if (commandLine.hasOption("h")) {
                new HelpFormatter().printHelp("text-server [<options> ...]\n", OPTIONS);
                return;
            }

            new Server().configure(commandLine).run();
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, t.getMessage(), t);
        }
    }

    Server configure(CommandLine commandLine) {
        final String contextPath = commandLine.getOptionValue("cp", "/").replaceAll("/+$", "");

        final String yuiRootDir = System.getProperty("yui.dir", "");
        Preconditions.checkArgument(yuiRootDir.isEmpty() || new File(yuiRootDir).isDirectory(), yuiRootDir);

        final File templateDir = new File(System.getProperty("template.dir", "template"));
        Preconditions.checkArgument(templateDir.isDirectory(), "Templates: " + templateDir);

        final File assetDir = new File(System.getProperty("asset.dir", "asset"));
        Preconditions.checkArgument(assetDir.isDirectory(), "Assets: " + assetDir);

        this.configuration = new Configuration();
        configuration.put("contextPath", contextPath);
        configuration.put("assetRoot", assetDir.getPath());
        configuration.put("assetPath", contextPath + "/asset");
        configuration.put("yuiRoot", yuiRootDir);
        configuration.put("yuiPath", yuiRootDir.isEmpty() ? "http://yui.yahooapis.com/3.9.1/build" : (contextPath + "/yui"));
        configuration.put("dataDirectory", dataDirectory(commandLine));
        configuration.put("httpPort", Integer.parseInt(commandLine.getOptionValue("p", "7369")));
        configuration.put("templatePath", templateDir);

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine(Joiner.on("\n").join(Iterables.concat(Collections.singleton("Configuration:"), configuration.entrySet())));
        }

        return this;
    }

    @Provides
    public Configuration getConfiguration() {
        return configuration;
    }

    @Provides
    public DataSource getDatabase() {
        return Database.h2((File) configuration.get("dataDirectory"));
    }

    @Provides
    public ObjectMapper getObjectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new TextModule());
        return objectMapper;
    }

    @Provides
    public Transactions transactions(DataSource dataSource, ObjectMapper objectMapper) {
        return new Transactions(dataSource, objectMapper);
    }

    @Provides
    public DefaultResourceConfig httpResourceConfig(IndexResource indexResource, TextResource textResource, TextExtractionResource extractionResource) {
        final DefaultResourceConfig rc = new DefaultResourceConfig();

        final HashMap<String,Object> config = Maps.newHashMap();
        config.put(DefaultResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS, Arrays.<Class<?>>asList(NormalizeFilter.class, GZIPContentEncodingFilter.class));
        config.put(DefaultResourceConfig.PROPERTY_CONTAINER_RESPONSE_FILTERS, Arrays.<Class<?>>asList(GZIPContentEncodingFilter.class));
        config.put(DefaultResourceConfig.FEATURE_CANONICALIZE_URI_PATH, true);
        config.put(DefaultResourceConfig.FEATURE_NORMALIZE_URI, true);
        config.put(DefaultResourceConfig.FEATURE_REDIRECT, true);
        rc.setPropertiesAndFeatures(config);
        rc.getSingletons().add(indexResource);
        rc.getSingletons().add(textResource);
        rc.getSingletons().add(extractionResource);

        return rc;
    }

    @Override
    public void run() {
        final ServiceManager serviceManager = new ServiceManager(Arrays.asList(ObjectGraph.create(this).get(HttpService.class)));
        serviceManager.addListener(new ServiceManager.Listener() {
            public void stopped() {}
            public void healthy() {}
            public void failure(Service service) {
                System.exit(1);
            }
        }, MoreExecutors.sameThreadExecutor());

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    serviceManager.stopAsync().awaitStopped(5, TimeUnit.SECONDS);
                } catch (TimeoutException timeout) {
                }
            }
        });
        serviceManager.startAsync();

        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
    }

    protected File dataDirectory(CommandLine commandLine) {
        final File dataDirectoryPath = Objects.firstNonNull(
                (commandLine.hasOption("d") ? new File(commandLine.getOptionValue("d")) : null),
                new File(System.getProperty("user.dir"), "data")
        );
        if (!dataDirectoryPath.isDirectory() && !dataDirectoryPath.mkdirs()) {
            throw new IllegalArgumentException("Data directory '" + dataDirectoryPath + "'");
        }
        return dataDirectoryPath;
    }

    static final Options OPTIONS = new Options();

    static {
        OPTIONS.addOption("d", "dir", true, "data directory, defaults to ./data");
        OPTIONS.addOption("cp", "context-path", true, "URL base/context path of the service, default: '/'");
        OPTIONS.addOption("h", "help", false, "prints usage instructions");
        OPTIONS.addOption("p", "port", true, "HTTP port to bind server to, default: 7369");
    }

}
