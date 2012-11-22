package eu.interedition.text;

import java.io.Closeable;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public interface QueryResult<T> extends Iterable<Layer<T>>, Closeable {
}
