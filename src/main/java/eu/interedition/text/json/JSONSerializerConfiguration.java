package eu.interedition.text.json;

import eu.interedition.text.Query;
import eu.interedition.text.TextRange;
import java.net.URI;
import java.util.Map;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public interface JSONSerializerConfiguration {

  TextRange getRange();

  Map<String, URI> getNamespaceMappings();

  Query getQuery();
}
