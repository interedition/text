package eu.interedition.text.http;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import eu.interedition.text.http.io.ObjectMapperProvider;
import eu.interedition.text.IdentifierGenerator;
import eu.interedition.text.Transactions;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.codehaus.jackson.map.ObjectMapper;

import javax.sql.DataSource;
import java.io.File;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class Server implements Runnable {
    private static final Logger LOG = Logger.getLogger(Server.class.getName());

    Deque<Service> services = Lists.newLinkedList();
    Injector injector;

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

        final String templateDir = System.getProperty("template.dir", "");
        Preconditions.checkArgument(templateDir.isEmpty() || new File(templateDir).isDirectory(), templateDir);

        final String assetDir = Objects.firstNonNull(
                System.getProperty("asset.dir"),
                Strings.emptyToNull(getClass().getResource("/asset").getFile())
        );
        Preconditions.checkArgument(assetDir != null && new File(assetDir).isDirectory(), "Assets: " + assetDir);

        final Map<String, String> configuration = Maps.newHashMap();
        configuration.put("contextPath", contextPath);
        configuration.put("assetRoot", assetDir);
        configuration.put("assetPath", contextPath + "/asset");
        configuration.put("yuiRoot", yuiRootDir);
        configuration.put("yuiPath", yuiRootDir.isEmpty()
                ? "http://yui.yahooapis.com/3.9.1/build"
                : (contextPath + "/yui")
        );
        configuration.put("dataDirectory", dataDirectory(commandLine).getPath());
        configuration.put("httpPort", commandLine.getOptionValue("p", "7369"));
        configuration.put("templatePath", templateDir);

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine(Joiner.on("\n").join(Iterables.concat(Collections.singleton("Configuration:"), configuration.entrySet())));
        }
        injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                Names.bindProperties(binder(), configuration);
                bind(ObjectMapper.class).toProvider(ObjectMapperProvider.class);
                bind(DataSource.class).toProvider(DataSourceProvider.class).asEagerSingleton();
                bind(Transactions.class).toProvider(TextTransactionsProvider.class).asEagerSingleton();
                bind(IdentifierGenerator.class).annotatedWith(Names.named("texts")).toProvider(new IdentifierGeneratorProvider("text"));
                bind(IdentifierGenerator.class).annotatedWith(Names.named("annotations")).toProvider(new IdentifierGeneratorProvider("annotation"));
            }
        });
        return this;
    }

    @Override
    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                final List<ListenableFuture<Service.State>> serviceStates = Lists.newLinkedList();
                for (Service service : services) {
                    serviceStates.add(service.stop());
                }
                for (ListenableFuture<Service.State> serviceState : serviceStates) {
                    try {
                        serviceState.get();
                    } catch (InterruptedException e) {
                    } catch (ExecutionException e) {
                    }
                }
            }
        }));

        start(HttpService.class);

        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
    }

    protected void start(Class<? extends Service> service) {
        final Service instance = injector.getInstance(service);
        instance.start();
        services.push(instance);
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
