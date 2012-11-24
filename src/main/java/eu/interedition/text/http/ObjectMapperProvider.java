package eu.interedition.text.http;

import com.google.inject.Provider;
import eu.interedition.text.json.TextModule;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class ObjectMapperProvider implements Provider<ObjectMapper> {

    @Override
    public ObjectMapper get() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new TextModule());
        return objectMapper;
    }
}
