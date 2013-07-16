package eu.interedition.text;

import com.google.common.base.Throwables;
import java.io.IOException;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public interface TextStream<T> {

    void stream(Listener<T> listener) throws IOException;

    interface Listener<T> {

        void start(long contentLength);

        void start(long offset, Iterable<Layer<T>> layers);

        void end(long offset, Iterable<Layer<T>> layers);

        void text(TextRange r, String text);

        void end();
    }

    /**
     * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
     */
    class ListenerAdapter<T> implements Listener<T> {
        public void start(long contentLength) {
        }

        public void start(long offset, Iterable<Layer<T>> layers) {
        }

        public void end(long offset, Iterable<Layer<T>> layers) {
        }

        public void text(TextRange r, String text) {
        }

        public void end() {
        }
    }

    /**
     * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
     */
    class ExceptionPropagatingListenerAdapter<T> implements Listener<T> {
      public void start(long contentLength) {
        try {
          doStart(contentLength);
        } catch (Exception e) {
          throw Throwables.propagate(e);
        }
      }

      public void start(long offset, Iterable<Layer<T>> layers) {
        try {
          doStart(offset, layers);
        } catch (Exception e) {
          throw Throwables.propagate(e);
        }
      }

      public void end(long offset, Iterable<Layer<T>> layers) {
        try {
          doEnd(offset, layers);
        } catch (Exception e) {
          throw Throwables.propagate(e);
        }
      }

      public void text(TextRange r, String text) {
        try {
          doText(r, text);
        } catch (Exception e) {
          throw Throwables.propagate(e);
        }
      }

      public void end() {
        try {
          doEnd();
        } catch (Exception e) {
          throw Throwables.propagate(e);
        }
      }

      protected void doStart(long contentLength) throws Exception {
      }

      protected void doStart(long offset, Iterable<Layer<T>> layers) throws Exception {
      }

      protected void doEnd(long offset, Iterable<Layer<T>> layers) throws Exception {
      }

      protected void doText(TextRange r, String text) throws Exception {
      }

      protected void doEnd() throws Exception {
      }
    }
}
