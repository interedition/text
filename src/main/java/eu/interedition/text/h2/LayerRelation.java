package eu.interedition.text.h2;

import eu.interedition.text.Anchor;
import eu.interedition.text.Layer;
import eu.interedition.text.Name;
import eu.interedition.text.TextRange;
import java.io.Reader;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class LayerRelation<T> implements Layer<T> {

    private final Name name;
    private final Set<Anchor> anchors;
    private final long id;
    private final H2TextRepository<T> repository;

    public LayerRelation(Name name, Set<Anchor> anchors, long id, H2TextRepository<T> repository) {
        this.name = name;
        this.anchors = anchors;
        this.id = id;
        this.repository = repository;
    }

    public long getId() {
        return id;
    }

    @Override
    public Name getName() {
        return name;
    }

    @Override
    public Set<Anchor> getAnchors() {
        return anchors;
    }

    @Override
    public Reader read() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Reader read(TextRange range) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public T getData() {
        return null;
    }

    @Override
    public SortedMap<TextRange, String> read(SortedSet<TextRange> textRanges) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long length() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
