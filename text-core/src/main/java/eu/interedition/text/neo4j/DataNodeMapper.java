package eu.interedition.text.neo4j;

import java.io.IOException;
import org.neo4j.graphdb.Node;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public interface DataNodeMapper<T> {

    T read(Node source, Class<T> type) throws IOException;

    void write(T data, Node target) throws IOException;
}
