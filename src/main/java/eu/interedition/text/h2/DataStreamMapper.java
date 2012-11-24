package eu.interedition.text.h2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public interface DataStreamMapper<T> {

    T read(InputStream stream, Class<T> type) throws IOException;

    void write(T data, OutputStream stream) throws IOException;
}
