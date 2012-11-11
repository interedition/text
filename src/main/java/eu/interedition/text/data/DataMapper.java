package eu.interedition.text.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public interface DataMapper<T> {

    void map(T data, OutputStream stream) throws IOException;

    T unmap(InputStream stream, Class<T> type) throws IOException;
}
