package eu.interedition.text.neo4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.neo4j.graphdb.Node;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class SerializeDataNodeMapper<T> implements DataNodeMapper<T> {

    private static final String DATA = "data";

    @SuppressWarnings("unchecked")
    @Override
    public T read(Node source, Class<T> type) throws IOException {
        final byte[] data = (byte[]) source.getProperty(DATA, null);
        if (data == null) {
            return null;
        }
        final ObjectInputStream objectStream = new ObjectInputStream(new ByteArrayInputStream(data));
        try {
            return (T) objectStream.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void write(T data, Node target) throws IOException {
        if (data == null) {
            target.removeProperty(DATA);
        }

        final ByteArrayOutputStream dataBuf = new ByteArrayOutputStream();

        final ObjectOutputStream objectStream = new ObjectOutputStream(dataBuf);
        objectStream.writeObject(data);
        objectStream.flush();

        target.setProperty(DATA, dataBuf.toByteArray());
    }
}
