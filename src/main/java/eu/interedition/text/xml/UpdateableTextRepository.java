package eu.interedition.text.xml;

import eu.interedition.text.Layer;
import java.io.IOException;
import java.io.Reader;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public interface UpdateableTextRepository<T> {

    void updateText(Layer<T> target, Reader text) throws IOException;
}
