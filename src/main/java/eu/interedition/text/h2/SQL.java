package eu.interedition.text.h2;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
class SQL {

    private static void close(AutoCloseable closeable) throws Exception {
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
