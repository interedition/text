package eu.interedition.text.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class SerializableDataMapper<T> implements DataMapper<T> {

    @Override
    public void serialize(T data, OutputStream stream) throws IOException {
        final ObjectOutputStream objectStream = new ObjectOutputStream(stream);
        objectStream.writeObject(data);
        objectStream.flush();
    }

    @SuppressWarnings("unchecked")
    @Override
    public T deserialize(InputStream stream, Class<T> type) throws IOException {
        final ObjectInputStream objectStream = new ObjectInputStream(stream);
        try {
            return (T) objectStream.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }
}
