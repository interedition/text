package eu.interedition.text.simple;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class KeyValues extends HashMap<String,Object> {

    public KeyValues() {
    }

    public KeyValues(Map<? extends String, ?> m) {
        super(m);
    }

}
