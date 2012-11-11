package eu.interedition.text.h2;

import java.sql.SQLException;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class SQL {

    public static void close(AutoCloseable closeable) throws Exception {
        if (closeable != null) {
            closeable.close();
        }
    }

    public static void closeQuietly(AutoCloseable closeable) {
        try {
            close(closeable);
        }catch (Exception e) {
        }
    }
}
