package eu.interedition.text;

import eu.interedition.text.repository.Store;

import java.util.Iterator;

/**
* @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
*/
public interface Repository {

    Iterator<Long> textIds();

    Iterator<Long> annotationIds();

    <R> R execute(Transaction<R> tx);

    abstract class Transaction<R> {

        public abstract R transactional(Store store);

        public boolean isReadOnly() {
            return false;
        }
    }
}
