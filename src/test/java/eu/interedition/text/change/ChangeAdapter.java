package eu.interedition.text.change;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import eu.interedition.text.Layer;
import eu.interedition.text.Name;
import eu.interedition.text.simple.KeyValues;
import java.net.URI;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class ChangeAdapter {
    private static final String REVISION_TYPE = new Name(URI.create("http://www.faustedition.net/ns"), "revType").toString();
    private static final String CHANGE_SET_REF = new Name(URI.create("http://www.tei-c.org/ns/geneticEditions"), "stage").toString();

    private final Layer<KeyValues> annotation;
    private String revisionType;
    private String changeSetRef;

    public ChangeAdapter(Layer<KeyValues> annotation) {
        this.annotation = annotation;
    }

    public Layer<KeyValues> getAnnotation() {
        return annotation;
    }

    public String getChangeSetRef() {
        if (changeSetRef != null) {
            return changeSetRef;
        }

        final KeyValues data = annotation.data();
        if (data != null && data.containsKey(CHANGE_SET_REF)) {
            changeSetRef = data.get(CHANGE_SET_REF).toString().replaceAll("^#", "");
        }

        return changeSetRef;
    }

    public void setChangeSetRef(String changeSetRef) {
        this.changeSetRef = changeSetRef;
    }

    public String getRevisionType() {
        if (revisionType != null) {
            return revisionType;
        }
        final KeyValues data = annotation.data();
        if (data != null && data.containsKey(REVISION_TYPE)) {
            revisionType = data.get(REVISION_TYPE).toString();
        }

        return revisionType;
    }

    public void setRevisionType(String revisionType) {
        this.revisionType = revisionType;
    }

    @Override
    public int hashCode() {
        return annotation.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof ChangeAdapter) {
            return annotation.equals(((ChangeAdapter) obj).annotation);
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return annotation.toString();
    }

    static final Function<Layer<KeyValues>, ChangeAdapter> ADAPT = new Function<Layer<KeyValues>, ChangeAdapter>() {
        @Override
        public ChangeAdapter apply(Layer<KeyValues> input) {
            return new ChangeAdapter(input);
        }
    };

    static final Function<ChangeAdapter, Layer<KeyValues>> TO_ANNOTATION = new Function<ChangeAdapter, Layer<KeyValues>>() {
        @Override
        public Layer<KeyValues> apply(ChangeAdapter input) {
            return input.getAnnotation();
        }
    };

    static final Function<ChangeAdapter, String> TO_REV_TYPE = new Function<ChangeAdapter, String>() {
        @Override
        public String apply(ChangeAdapter input) {
            return Objects.firstNonNull(input.getRevisionType(), "");
        }
    };

    static final Predicate<ChangeAdapter> HAS_CHANGE_SET_REF = new Predicate<ChangeAdapter>() {
        @Override
        public boolean apply(ChangeAdapter input) {
            return !Strings.isNullOrEmpty(input.getChangeSetRef());
        }
    };

    static final Predicate<ChangeAdapter> HAS_REV_TYPE = new Predicate<ChangeAdapter>() {
        @Override
        public boolean apply(ChangeAdapter input) {
            return !Strings.isNullOrEmpty(input.getRevisionType());
        }
    };
}
