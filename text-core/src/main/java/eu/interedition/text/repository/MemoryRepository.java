package eu.interedition.text.repository;

import com.google.common.collect.AbstractIterator;
import eu.interedition.text.Repository;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class MemoryRepository implements Repository {

    private final MemoryStore store;
    private final Iterator<Long> textIds = new IdGenerator();
    private final Iterator<Long> annotationIds = new IdGenerator();

    public MemoryRepository(MemoryStore store) {
        this.store = store;
    }

    @Override
    public Iterator<Long> textIds() {
        return textIds;
    }

    @Override
    public Iterator<Long> annotationIds() {
        return annotationIds;
    }

    @Override
    public <R> R execute(Transaction<R> tx) {
        synchronized (store) {
            return tx.transactional(store);
        }
    }

    private static class IdGenerator extends AbstractIterator<Long> {

        private final AtomicLong ids = new AtomicLong();

        @Override
        protected Long computeNext() {
            return ids.incrementAndGet();
        }
    }
}
