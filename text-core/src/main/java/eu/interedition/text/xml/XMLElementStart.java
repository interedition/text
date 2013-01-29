package eu.interedition.text.xml;

import eu.interedition.text.Name;
import java.util.Map;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class XMLElementStart {

    private final Name name;
    private final Map<Name, String> attributes;
    private final long offset;

    public XMLElementStart(Name name, Map<Name, String> attributes, long offset) {
        this.name = name;
        this.attributes = attributes;
        this.offset = offset;
    }

    public Name getName() {
        return name;
    }

    public Map<Name, String> getAttributes() {
        return attributes;
    }

    public long getOffset() {
        return offset;
    }
}
