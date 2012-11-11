/*
 * #%L
 * Text Repository: Datastore for texts based on Interedition's model.
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
package eu.interedition.text.token;

import eu.interedition.text.Layer;
import eu.interedition.text.TextRange;
import eu.interedition.text.TextStream;
import java.util.logging.Logger;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class Tokenizer<T> implements TextStream.Listener<T> {
    private final Logger LOG = Logger.getLogger(getClass().getName());
    private final TokenizerSettings<T> settings;

    private boolean afterTokenBoundary = true;
    private int offset = 0;
    private int tokenStart = Integer.MAX_VALUE;
    private int tokenCount = 0;

    public Tokenizer(TokenizerSettings<T> settings) {
        this.settings = settings;
    }

    @Override
    public void start(long contentLength) {
    }

    @Override
    public void start(long offset, Iterable<Layer<T>> annotations) {
        if (settings.startingAnnotationsAreBoundary(this, offset, annotations)) {
            afterTokenBoundary = true;
        }
    }

    @Override
    public void end(long offset, Iterable<Layer<T>> annotations) {
        if (settings.endingAnnotationsAreBoundary(this, offset, annotations)) {
            afterTokenBoundary = true;
        }
    }

    @Override
    public void text(TextRange r, String content) {
        for (char c : content.toCharArray()) {
            if (settings.isBoundary(this, offset, c)) {
                afterTokenBoundary = true;
            } else {
                if (afterTokenBoundary) {
                    token();
                }
                if (tokenStart > offset) {
                    tokenStart = offset;
                }
                afterTokenBoundary = false;
            }

            offset++;
        }
    }

    @Override
    public void end() {
        token();
    }

    protected void token() {
        if (tokenStart < offset) {
            settings.token(this, new TextRange(tokenStart, offset));
            tokenCount++;
            tokenStart = Integer.MAX_VALUE;
        }
    }

    public TokenizerSettings getSettings() {
        return settings;
    }

    public boolean isAfterTokenBoundary() {
        return afterTokenBoundary;
    }

    public int getOffset() {
        return offset;
    }

    public int getTokenStart() {
        return tokenStart;
    }

    public int getTokenCount() {
        return tokenCount;
    }
}
