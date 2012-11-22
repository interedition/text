package eu.interedition.text.lisp;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class NumberAtom implements Expression {

    private final long value;

    public NumberAtom(long value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }
}
