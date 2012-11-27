/*
 * #%L
 * Text: A text model with range-based markup via standoff annotations.
 * %%
 * Copyright (C) 2010 - 2011 The Interedition Development Group
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package eu.interedition.text;

import com.google.common.io.Files;
import eu.interedition.text.h2.H2TextRepository;
import eu.interedition.text.h2.SerializableDataStreamMapper;
import eu.interedition.text.neo4j.Neo4jTextRepository;
import eu.interedition.text.neo4j.SerializableDataNodeMapper;
import eu.interedition.text.simple.KeyValues;
import eu.interedition.text.simple.SimpleTextRepository;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.Collections;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.EmbeddedGraphDatabase;

/**
 * Base class for tests using an in-memory document model.
 *
 * @author <a href="http://gregor.middell.net/"
 *         title="Homepage of Gregor Middell">Gregor Middell</a>
 */
public abstract class AbstractTextTest extends AbstractTest {

    protected static final String TEST_TEXT = "Hello World";

    private static H2TextRepository<KeyValues> h2Repository;
    private static DataSource h2DataSource;

    private static Neo4jTextRepository<KeyValues> graphRepository;
    private static File graphTempDataDir;
    private static EmbeddedGraphDatabase graphDatabase;

    protected TextRepository<KeyValues> repository;

    /**
     * The in-memory document model to run tests against.
     */
    private Layer<KeyValues> layer;
    private Transaction graphTransaction;

    /**
     * Creates a new document model before every test.
     */
    public Layer<KeyValues> testText() throws IOException {
        if (layer == null) {
            this.layer = repository.add(new Name(TextConstants.INTEREDITION_NS_URI, "test"), new StringReader(getTestText()), null);
        }
        return layer;
    }

    /**
     * Removes the document model.
     */
    @After
    public void cleanTestText() throws IOException {
        if (layer != null) {
            repository.delete(Collections.singleton(this.layer));
            layer = null;
        }
    }

    String getTestText() {
        return TEST_TEXT;
    }

    @Before
    public void initRepository() throws SQLException, IOException {
        final String repo = System.getProperty("interedition.text.repo", "mem").toLowerCase();
        if ("mem".equals(repo)) {
            repository = new SimpleTextRepository<KeyValues>();
        } else if ("h2".equals(repo)) {
            repository = h2Repository();
        } else if ("neo4j".equals(repo)) {
            repository = graphRepository();
            graphTransaction = graphDatabase.beginTx();
        } else {
            throw new IllegalArgumentException(repo);
        }
    }

    private static Neo4jTextRepository<KeyValues> graphRepository() throws IOException {
        if (graphRepository == null) {
            graphTempDataDir = Files.createTempDir();
            graphDatabase = new EmbeddedGraphDatabase(graphTempDataDir.getCanonicalPath());
            graphRepository = new Neo4jTextRepository<KeyValues>(KeyValues.class, new SerializableDataNodeMapper<KeyValues>(), graphDatabase, false);
        }

        return graphRepository;
    }

    private static H2TextRepository<KeyValues> h2Repository() throws SQLException {
        if (h2Repository == null) {
            final StringBuilder url = new StringBuilder("jdbc:h2:mem:text;DB_CLOSE_DELAY=-1");
            if (System.getProperty("interedition.debug") != null) {
                url.append(";TRACE_LEVEL_SYSTEM_OUT=2");
            }
            h2DataSource = JdbcConnectionPool.create(url.toString(), "sa", "");
            h2Repository = new H2TextRepository<KeyValues>(KeyValues.class, new SerializableDataStreamMapper<KeyValues>(), h2DataSource, false).withSchema();
        }
        return h2Repository;
    }

    @AfterClass
    public static void closeDataSource() {
        h2Repository = null;
        h2DataSource = null;
        if (graphRepository != null) {
            graphRepository = null;

            graphDatabase.shutdown();
            graphDatabase = null;

            delete(graphTempDataDir);
            graphTempDataDir = null;
        }
    }

    @After
    public void rollback() throws SQLException {
        if (h2Repository != null) {
            h2Repository.clearNameCache();
        }
        if (graphTransaction != null) {
            graphTransaction.finish();
            graphTransaction = null;
        }
    }

    private static void delete(File file) {
        if (file.isDirectory()) {
            for (File contained : file.listFiles()) {
                delete(contained);
            }
        }
        file.delete();
    }

}
