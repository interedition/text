package eu.interedition.text.util;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class AutoCloseables {
    private static final Logger LOGGER = Logger.getLogger(AutoCloseables.class.getName());

    public static void close(AutoCloseable closeable) throws Exception {
        if (closeable != null) {
            closeable.close();
        }
    }

    public static void closeQuietly(AutoCloseable closeable) {
        try {
            close(closeable);
        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING, MessageFormat.format("Exception while closing {0}", closeable), e);
            }
        }
    }
}
