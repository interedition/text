/*
 * Copyright (c) 2013 The Interedition Development Group.
 *
 * This file is part of CollateX.
 *
 * CollateX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CollateX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CollateX.  If not, see <http://www.gnu.org/licenses/>.
 */

package eu.interedition.text.index;

import com.google.common.base.Throwables;
import com.google.common.io.Closer;
import com.google.common.util.concurrent.AbstractScheduledService;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public abstract class IndexServiceBase extends AbstractScheduledService {
    private final Logger LOG = Logger.getLogger(getClass().getName());

    private Directory directory;
    private SearcherManager searchManager;

    public SearcherManager searchManager() throws IOException {
        synchronized (this) {
            if (searchManager == null) {
                searchManager = new SearcherManager(directory(), new SearcherFactory());
            }
        }
        return searchManager;
    }

    public IndexWriter writer() throws IOException {
        return new IndexWriter(directory(), standardIndexWriterConfig());
    }

    public DirectoryReader reader() throws IOException {
        return DirectoryReader.open(directory());
    }

    protected Directory directory() throws IOException {
        synchronized (this) {
            if (directory == null) {
                directory = createDirectory();
            }
        }
        return directory;
    }

    protected abstract Directory createDirectory() throws IOException;

    public boolean isEmpty() {
        final Closer closer = Closer.create();
        try {
            return (closer.register(reader()).numDocs() == 0);
        } catch (IOException e) {
            if (LOG.isLoggable(Level.SEVERE)) {
                LOG.log(Level.SEVERE, "I/O while checking whether index is empty; assuming it is", e);
            }
            return true;
        } finally {
            try {
                closer.close();
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }

    }

    protected IndexWriterConfig standardIndexWriterConfig() {
        return new IndexWriterConfig(Version.LUCENE_42, new StandardAnalyzer(Version.LUCENE_42))
                .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND).setWriteLockTimeout(30000);
    }


    @Override
    protected void startUp() throws Exception {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "Starting {0}", getClass().getName());
        }
        final Closer closer = Closer.create();
        try {
            closer.register(writer()).commit();
        } finally {
            closer.close();
        }
    }

    @Override
    protected void shutDown() throws Exception {
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "Stopping index service");
        }
        synchronized (this) {
            searchManager.close();
        }
    }

    @Override
    protected void runOneIteration() throws Exception {
        synchronized (this) {
            if (searchManager != null) {
                if (LOG.isLoggable(Level.FINER)) {
                    LOG.log(Level.FINER, "Refreshing index search manager(s)");
                }
                try {
                    searchManager.maybeRefresh();
                } catch (IOException e) {
                }
            }
        }
    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(0, 60, TimeUnit.SECONDS);
    }
}