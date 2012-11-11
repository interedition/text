package eu.interedition.text;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public interface Text {

    void read(Writer target) throws IOException;

    void read(TextRange range, Writer target) throws IOException;

    Reader read() throws IOException;

    Reader read(TextRange range) throws IOException;

    SortedMap<TextRange,String> read(SortedSet<TextRange> textRanges);

    long length();

}
