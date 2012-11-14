package eu.interedition.text.util;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public interface RestorableTextRepository {

    void backup(Writer to) throws IOException;

    void restore(Reader from) throws IOException;

    void restore(File from, Charset charset) throws IOException;

}
