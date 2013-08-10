package eu.interedition.text.repository;

import com.google.common.collect.AbstractIterator;
import com.google.common.util.concurrent.MoreExecutors;
import eu.interedition.text.Repository;

import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class SimpleRepository implements Repository {

    private final SimpleStore store;
    private final Iterable<Listener> listeners;
    private final Iterator<Long> textIds = new IdGenerator();
    private final Iterator<Long> annotationIds = new IdGenerator();

    private ExecutorService listenerExecutorService = MoreExecutors.sameThreadExecutor();

    public SimpleRepository(SimpleStore store, Listener... listeners) {
        this(store, Arrays.asList(listeners));
    }

    public SimpleRepository(SimpleStore store, Iterable<Listener> listeners) {
        this.store = store;
        this.listeners = listeners;
    }

    public SimpleRepository withListenerExecutorService(ExecutorService listenerExecutorService) {
        this.listenerExecutorService = listenerExecutorService;
        return this;
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
            final R result = tx.transactional(store);
            store.txLog().notify(listenerExecutorService, listeners).clear();
            return result;
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
