package eu.interedition.text;

import java.util.logging.Level;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import org.junit.Test;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class ScriptingTest extends AbstractTest {

    private final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();

    @Test
    public void retrieveScriptEngines() {
        for (ScriptEngineFactory sef : scriptEngineManager.getEngineFactories()) {
            LOG.log(Level.FINE, "{} {} :: {} {}", new Object[]{sef.getEngineName(), sef.getEngineVersion(), sef.getLanguageName(), sef.getLanguageVersion()});
            for (String name : sef.getNames()) {
                LOG.log(Level.FINE, "\t{}", name);
            }
        }
    }

    @Test
    public void ecmaScript() throws ScriptException {
        final ScriptEngine scriptEngine = scriptEngineManager.getEngineByName("ECMAScript");
        final CompiledScript script = ((Compilable) scriptEngine).compile("a + b");

        final SimpleBindings bindings = new SimpleBindings();
        bindings.put("a", 1);
        bindings.put("b", 2);
        LOG.fine(script.eval(bindings).toString());
    }
}
