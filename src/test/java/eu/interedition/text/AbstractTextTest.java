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

import eu.interedition.text.h2.H2TextRepository;
import eu.interedition.text.simple.KeyValues;
import eu.interedition.text.simple.SimpleTextRepository;
import eu.interedition.text.util.AutoCloseables;
import java.io.IOException;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.Collections;
import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;

/**
 * Base class for tests using an in-memory document model.
 *
 * @author <a href="http://gregor.middell.net/"
 *         title="Homepage of Gregor Middell">Gregor Middell</a>
 */
public abstract class AbstractTextTest extends AbstractTest {

    protected static final String TEST_TEXT = "Hello World";

    private static H2TextRepository<KeyValues> h2Repository;
    private static SingleConnectionDataSource h2DataSource;

    protected TextRepository<KeyValues> repository;

    /**
     * The in-memory document model to run tests against.
     */
    private Layer<KeyValues> layer;

    private static H2TextRepository<KeyValues> h2Repository() throws SQLException {
        if (h2Repository == null) {
            final StringBuilder url = new StringBuilder("jdbc:h2:mem:text;DB_CLOSE_DELAY=-1");
            if (System.getProperty("interedition.debug") != null) {
                url.append(";TRACE_LEVEL_SYSTEM_OUT=2");
            }
            h2DataSource = new SingleConnectionDataSource(JdbcConnectionPool.create(url.toString(), "sa", "sa"));
            h2Repository = new H2TextRepository<KeyValues>(KeyValues.class, h2DataSource, false).withSchema();
        }
        return h2Repository;
    }

    @AfterClass
    public static void closeDataSource() {
        h2Repository = null;
        if (h2DataSource != null && h2DataSource.connection != null) {
            AutoCloseables.closeQuietly(h2DataSource.connection);
        }
    }

    @After
    public void rollback() throws SQLException {
        if (h2DataSource != null && h2DataSource.connection != null) {
            h2DataSource.connection.rollback();
        }
        if (h2Repository != null) {
            h2Repository.clearNameCache();
        }
    }

    /**
     * Creates a new document model before every test.
     */
    public Layer<KeyValues> testText() throws IOException {
        if (layer == null) {
            this.layer = repository.add(new Name(TextConstants.INTEREDITION_NS_URI, "test"), new StringReader(getTestText()), null);
        }
        return layer;
    }

    @Before
    public void initRepository() throws SQLException {
        final String repo = System.getProperty("interedition.text.repo", "mem").toLowerCase();
        if ("mem".equals(repo)) {
            repository = new SimpleTextRepository<KeyValues>();
        } else if ("h2".equals(repo)) {
            repository = h2Repository();
        } else {
            throw new IllegalArgumentException(repo);
        }
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
}
