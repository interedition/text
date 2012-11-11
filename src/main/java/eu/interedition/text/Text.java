package eu.interedition.text;

import java.io.Reader;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public interface Text {

    Reader read();

    Reader read(TextRange range);

    SortedMap<TextRange,String> read(SortedSet<TextRange> textRanges);

    long length();

}
