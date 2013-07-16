package eu.interedition.text.util;

import eu.interedition.text.Layer;
import java.io.IOException;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public interface BatchLayerAdditionSupport<T> {

    Iterable<Layer<T>> add(Iterable<Layer<T>> batch) throws IOException;
}
