package eu.interedition.text;

import java.io.IOException;
import java.io.Reader;
import java.util.Set;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public interface TextRepository<T> {

    Layer<T> findByIdentifier(long id);

    QueryResult<T> query(Query query);

    Layer<T> add(Name name, Reader text, T data, Set<Anchor<T>> anchors) throws IOException;

    Layer<T> add(Name name, Reader text, T data, Anchor<T> anchor) throws IOException;

    void delete(Iterable<Layer<T>> layers);
}
