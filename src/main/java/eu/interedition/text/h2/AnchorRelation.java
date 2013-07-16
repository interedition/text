package eu.interedition.text.h2;

import com.google.common.base.Objects;
import eu.interedition.text.Anchor;
import eu.interedition.text.Layer;
import eu.interedition.text.Text;
import eu.interedition.text.TextRange;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class AnchorRelation<T> extends Anchor<T> {

    private final long id;

    public AnchorRelation(Layer<T> text, TextRange range, long id) {
        super(text, range);
        this.id = id;
    }

    public long getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof AnchorRelation) {
            return id == ((AnchorRelation) obj).id;
        }
        return super.equals(obj);
    }
}
