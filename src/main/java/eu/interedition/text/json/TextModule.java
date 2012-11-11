package eu.interedition.text.json;

import eu.interedition.text.Name;
import eu.interedition.text.TextRange;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.module.SimpleModule;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class TextModule extends SimpleModule {

  public TextModule() {
    super(TextModule.class.getPackage().getName(), Version.unknownVersion());
    addSerializer(new TextSerializer());
    addSerializer(new NameSerializer());
    addSerializer(new AnnotationSerializer());

    addDeserializer(Name.class, new NameDeserializer());
    addDeserializer(TextRange.class, new RangeDeserializer());
  }

}
