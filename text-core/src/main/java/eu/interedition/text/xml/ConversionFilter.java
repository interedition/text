package eu.interedition.text.xml;

import com.google.common.base.Predicate;

import javax.annotation.Nullable;
import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLStreamReader;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class ConversionFilter implements StreamFilter {

    Converter converter;

    protected Converter converter() {
        return converter;
    }

    public void init(Converter converter) {
        this.converter = converter;
    }

    public void start() {
    }

    public void end() {
    }

    protected void onXMLEvent(XMLStreamReader reader) {
    }

    protected String text(String text) {
        return "";
    }

    protected boolean callBeforeTextGeneration() {
        return false;
    }

    @Override
    public boolean accept(XMLStreamReader reader) {
        onXMLEvent(reader);
        return true;
    }

    public static final Predicate<ConversionFilter> BEFORE_TEXT_GENERATION = new Predicate<ConversionFilter>() {
        @Override
        public boolean apply(@Nullable ConversionFilter input) {
            return input.callBeforeTextGeneration();
        }
    };
}
