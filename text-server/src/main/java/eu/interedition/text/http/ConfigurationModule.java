package eu.interedition.text.http;

import com.google.common.io.Closeables;
import com.google.inject.AbstractModule;
import com.google.inject.ProvisionException;
import com.google.inject.name.Names;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class ConfigurationModule extends AbstractModule {
    private static final Logger LOG = Logger.getLogger(ConfigurationModule.class.getName());

    @Override
    protected void configure() {
        Names.bindProperties(binder(), readConfiguration());
    }

    private Properties readConfiguration() {
        final Properties configuration = new Properties();
        InputStream inputStream = null;
        try {
            configuration.load(inputStream = getClass().getResourceAsStream("/config.properties"));
            configuration.putAll(System.getProperties());
            detectConfigurationSettings(configuration);

            return configuration;
        } catch (IOException e) {
            throw new ProvisionException("I/O error while reading configuration", e);
        } finally {
            Closeables.closeQuietly(inputStream);
        }
    }

    protected void detectConfigurationSettings(Properties configuration) {
        if (!configuration.containsKey("interedition.data")) {
            String dataDirectoryPath;

            final File userHome = new File(System.getProperty("user.home"));
            final String osName = System.getProperty("os.name").toLowerCase();

            if (osName.contains("mac os x")) {
                dataDirectoryPath = new File(userHome, "Library/Application Support/Interedition").getAbsolutePath();
            } else if (osName.contains("windows")) {
                dataDirectoryPath = new File(userHome, "Application Data/Interedition").getAbsolutePath();
            } else {
                dataDirectoryPath = new File(userHome, ".interedition").getAbsolutePath();
            }

            final File dataDirectory = new File(dataDirectoryPath);
            if (!dataDirectory.isDirectory() && !dataDirectory.mkdirs()) {
                if (LOG.isLoggable(Level.WARNING)) {
                    LOG.warning("Cannot create data directory " + dataDirectory.getPath());
                }
            } else {
                if (LOG.isLoggable(Level.INFO)) {
                    LOG.info("Auto-detected data directory " + dataDirectory);
                }
                configuration.put("interedition.data", dataDirectory.getPath());
            }
        }
    }
}
