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

import com.google.common.base.Objects;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage of Gregor Middell">Gregor Middell</a>
 */
public class Anchor {

    private final Text text;
    private final TextRange range;

    public Anchor(Text text, TextRange range) {
        this.text = text;
        this.range = range;
    }

    public Text getText() {
        return text;
    }

    public TextRange getRange() {
        return range;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof Anchor) {
            final Anchor other = (Anchor) obj;
            return Objects.equal(text, other.getText()) && Objects.equal(range, other.getRange());
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(text, range);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).addValue(text).addValue(range).toString();
    }

}
