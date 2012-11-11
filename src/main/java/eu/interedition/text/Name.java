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
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.net.URI;
import java.util.Comparator;
import javax.xml.namespace.QName;

public class Name implements Comparable<Name> {

    protected final URI namespace;
    protected final String localName;

    public Name(URI namespace, String localName) {
        this.namespace = namespace;
        this.localName = localName;
    }

    public Name(QName name) {
        final String nsStr = Strings.emptyToNull(name.getNamespaceURI());
        this.namespace = (nsStr == null ? null : URI.create(nsStr));
        this.localName = name.getLocalPart();
    }

    public Name(String ns, String localName) {
        this(URI.create(ns), localName);
    }

    public URI getNamespace() {
        return namespace;
    }

    public String getLocalName() {
        return localName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof Name) {
            final Name other = (Name) obj;
            return Objects.equal(localName, other.getLocalName()) && Objects.equal(namespace, other.getNamespace());
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(localName, namespace);
    }

    @Override
    public int compareTo(Name o) {
        return COMPARATOR.compare(this, o);
    }

    @Override
    public String toString() {
        return (namespace == null ? localName : new StringBuilder("{").append(namespace).append("}").append(localName).toString());
    }

    public static Name fromString(String str) {
        if (!str.startsWith("{")) {
            return new Name((URI) null, str);
        }

        final int namespaceEnd = str.indexOf("}");
        Preconditions.checkArgument(namespaceEnd >= 1 && namespaceEnd < str.length() - 1);
        if (namespaceEnd == 1) {
            return new Name((URI) null, str);
        } else {
            return new Name(URI.create(str.substring(1, namespaceEnd)), str.substring(namespaceEnd + 1));
        }
    }


    public static final Comparator<Name> COMPARATOR = new Comparator<Name>() {

        public int compare(Name o1, Name o2) {
            final URI o1Ns = o1.getNamespace();
            final URI o2Ns = o2.getNamespace();

            final String o1LocalName = o1.getLocalName();
            final String o2LocalName = o2.getLocalName();

            if (o1Ns != null && o2Ns != null) {
                final int nsComp = o1Ns.compareTo(o2Ns);
                return (nsComp == 0 ? o1LocalName.compareTo(o2LocalName) : nsComp);
            } else if (o1Ns == null && o2Ns == null) {
                return o1LocalName.compareTo(o2LocalName);
            } else {
                return (o1Ns == null ? 1 : -1);
            }
        }
    };
}
