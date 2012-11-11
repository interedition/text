package eu.interedition.text.h2;

import eu.interedition.text.Name;
import java.net.URI;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class NameRelation extends Name {
    private final long id;

    public NameRelation(URI namespace, String localName, long id) {
        super(namespace, localName);
        this.id = id;
    }

    public NameRelation(Name name, long id) {
        this(name.getNamespace(), name.getLocalName(), id);
    }

    public NameRelation(String ns, String localName, long id) {
        this(new Name(ns, localName), id);
    }

    public long getId() {
        return id;
    }
}
