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
package eu.interedition.text.xml.module;

import eu.interedition.text.TextRange;
import eu.interedition.text.xml.XMLEntity;
import eu.interedition.text.xml.XMLTransformer;
import eu.interedition.text.xml.XMLTransformerModule;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class XMLTransformerModuleAdapter<T> implements XMLTransformerModule<T> {
  public void start(XMLTransformer<T> transformer) {
  }

  public void start(XMLTransformer<T> transformer, XMLEntity entity) {
  }

  public void startText(XMLTransformer<T> transformer) {
  }

  public void text(XMLTransformer<T> transformer, String text) {
  }

  public void textWritten(XMLTransformer<T> transformer, String read, String written) {
  }

  public void endText(XMLTransformer<T> transformer) {
  }

  public void end(XMLTransformer<T> transformer, XMLEntity entity) {
  }

  public void end(XMLTransformer<T> transformer) {
  }

  public void offsetMapping(XMLTransformer<T> transformer, TextRange textRange, TextRange sourceRange) {
  }
}
