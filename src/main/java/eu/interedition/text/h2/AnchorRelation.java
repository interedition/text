package eu.interedition.text.h2;

import eu.interedition.text.Anchor;
import eu.interedition.text.Text;
import eu.interedition.text.TextRange;
import java.util.Objects;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class AnchorRelation extends Anchor {

    private final long id;

    public AnchorRelation(Text text, TextRange range, long id) {
        super(text, range);
        this.id = id;
    }

    public long getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof AnchorRelation) {
            return id == ((AnchorRelation) obj).id;
        }
        return super.equals(obj);
    }
}
