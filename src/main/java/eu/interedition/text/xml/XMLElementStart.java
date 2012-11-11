package eu.interedition.text.xml;

import eu.interedition.text.Name;
import java.util.Map;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class XMLElementStart {

    final Name name;
    final Map<Name, Object> attributes;
    final long offset;

    public XMLElementStart(Name name, Map<Name, Object> attributes, long offset) {
        this.name = name;
        this.attributes = attributes;
        this.offset = offset;
    }

    public Name getName() {
        return name;
    }

    public Map<Name, Object> getAttributes() {
        return attributes;
    }

    public long getOffset() {
        return offset;
    }
}
