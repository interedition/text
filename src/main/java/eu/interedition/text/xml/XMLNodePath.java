package eu.interedition.text.xml;

import com.google.common.base.Objects;
import eu.interedition.text.Name;
import eu.interedition.text.TextConstants;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class XMLNodePath extends ArrayDeque<Integer> implements Comparable<XMLNodePath> {

    public XMLNodePath() {
        super(10);
    }

    public XMLNodePath(XMLNodePath nodePath) {
        super(nodePath);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof XMLNodePath) {
            return compareTo((XMLNodePath) obj) == 0;
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(toArray(new Object[size()]));
    }

    @Override
    public int compareTo(XMLNodePath o) {
        final Iterator<Integer> it = descendingIterator();
        final Iterator<Integer> otherIt = o.descendingIterator();

        int result;
        while (it.hasNext() && otherIt.hasNext()) {
            result = it.next().compareTo(otherIt.next());
            if (result != 0) {
                return result;
            }
        }

        if (it.hasNext()) {
            return 1;
        } else if (otherIt.hasNext()) {
            return -1;
        }
        return 0;
    }

    public void set(Map<Name, Object> attributes) {
        attributes.put(TextConstants.XML_NODE_ATTR_NAME, new XMLNodePath(this));
    }
}
