package eu.interedition.text.xml;

import com.google.common.collect.Range;

import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLStreamReader;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class TextExtractorComponent implements StreamFilter {

    TextExtractor extractor;

    protected TextExtractor extractor() {
        return extractor;
    }

    protected void onXMLEvent(XMLStreamReader reader) {
    }

    protected int text(String text) {
        return 0;
    }

    protected void map(Range<Integer> source, Range<Integer> text) {
    }

    protected boolean preTextExtraction() {
        return false;
    }

    @Override
    public boolean accept(XMLStreamReader reader) {
        onXMLEvent(reader);
        return true;
    }
}
