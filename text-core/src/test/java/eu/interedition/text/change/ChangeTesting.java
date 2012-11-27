package eu.interedition.text.change;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import eu.interedition.text.AbstractTestResourceTest;
import eu.interedition.text.Anchor;
import eu.interedition.text.Layer;
import eu.interedition.text.Name;
import eu.interedition.text.Query;
import eu.interedition.text.QueryResultTextStream;
import eu.interedition.text.Text;
import eu.interedition.text.TextConstants;
import eu.interedition.text.TextRange;
import eu.interedition.text.TextRanges;
import eu.interedition.text.TextStream;
import eu.interedition.text.simple.KeyValues;
import eu.interedition.text.xml.XML;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.junit.Test;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static eu.interedition.text.Query.name;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class ChangeTesting extends AbstractTestResourceTest {
    private static final File TEST_FILE_BASE = new File("/Users/gregor/Desktop/change-collection");
    private static final File OUTPUT_FILE = new File(TEST_FILE_BASE, "versions");

    @Test
    public void createTexts() throws IOException, XMLStreamException {
        if (!TEST_FILE_BASE.isDirectory()) {
            return;
        }

        if (OUTPUT_FILE.isDirectory()) {
            delete(OUTPUT_FILE);
        }
        OUTPUT_FILE.mkdir();

        for (File testFile : TEST_FILE_BASE.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isFile() && pathname.getName().endsWith(".xml");
            }
        })) {
            final List<ChangeSet> changeSets = Lists.newArrayList();

            source(testFile.toURI()).stream(new Text.Consumer() {
                @Override
                public void consume(Reader xmlStream) throws IOException {
                    XMLStreamReader xml = null;
                    try {
                        changeSets.addAll(ChangeSet.readDeclarations(xml = xmlInputFactory.createXMLStreamReader(xmlStream)));
                    } catch (XMLStreamException e) {
                        throw new IOException(e);
                    } finally {
                        XML.closeQuietly(xml);
                        Closeables.close(xmlStream, false);
                    }
                }
            });

            changeSets.add(0, new ChangeSet(""));

            final Layer testLayer = text(testFile.toURI());

            final Multimap<String, ChangeAdapter> changes = HashMultimap.create();
            new QueryResultTextStream<KeyValues>(repository, testLayer, Query.or(
                    name(new Name(TextConstants.TEI_NS, "add")),
                    name(new Name(TextConstants.TEI_NS, "del")),
                    name(new Name(TextConstants.TEI_NS, "subst")),
                    name(new Name(TextConstants.TEI_NS, "restore"))
            )).stream(new TextStream.ListenerAdapter<KeyValues>() {
                private Deque<ChangeAdapter> parents = new ArrayDeque<ChangeAdapter>();

                @Override
                public void start(long offset, Iterable<Layer<KeyValues>> annotations) {
                    for (Layer<KeyValues> annotation : TextRanges.orderingByTarget(testLayer).immutableSortedCopy(annotations)) {
                        final ChangeAdapter change = new ChangeAdapter(annotation);

                        final String revisionType = change.getRevisionType();
                        if (isNullOrEmpty(revisionType)) {
                            final ChangeAdapter parent = Iterators.find(parents.descendingIterator(), ChangeAdapter.HAS_REV_TYPE, null);
                            if (parent != null) {
                                change.setRevisionType(parent.getRevisionType());
                            }
                        }
                        if (isNullOrEmpty(change.getChangeSetRef())) {
                            final ChangeAdapter parent = Iterators.find(parents.descendingIterator(), ChangeAdapter.HAS_CHANGE_SET_REF, null);
                            if (parent != null) {
                                change.setChangeSetRef(parent.getChangeSetRef());
                            }
                            changes.put((parent == null ? "" : parent.getChangeSetRef()), change);
                        }
                        changes.put(firstNonNull(change.getChangeSetRef(), ""), change);

                        parents.add(change);
                    }
                }

                @Override
                public void end(long offset, Iterable<Layer<KeyValues>> annotations) {
                    parents.removeAll(Lists.newArrayList(Iterables.transform(annotations, ChangeAdapter.ADAPT)));
                }
            });

            final Multimap<String, ChangeAdapter> revTypeIndex = Multimaps.index(changes.values(), ChangeAdapter.TO_REV_TYPE);

            final HashSet<String> chosenRevisionTypes = Sets.newHashSet("soon-or-late", "late");

            Set<ChangeAdapter> before = Sets.newHashSet();
            Set<ChangeAdapter> after = Sets.newHashSet();
            for (String revisionType : revTypeIndex.keySet()) {
                (chosenRevisionTypes.contains(revisionType) ? after : before).addAll(revTypeIndex.get(revisionType));
            }

            final List<Set<ChangeAdapter>> versions = Lists.newArrayList();
            versions.add(before);
            versions.add(after);

            final Iterable<SortedSet<TextRange>> removedRanges = new Iterable<SortedSet<TextRange>>() {

                @Override
                public Iterator<SortedSet<TextRange>> iterator() {
                    return new AbstractIterator<SortedSet<TextRange>>() {
                        private int version = 0;

                        @Override
                        protected SortedSet<TextRange> computeNext() {
                            if (version > versions.size()) {
                                return endOfData();
                            }

                            final SortedSet<TextRange> rangesToRemove = Sets.newTreeSet();
                            String toRemove = "del";
                            for (int version = 0; version < versions.size(); version++) {
                                if (this.version <= version) {
                                    toRemove = "add";
                                }

                                for (ChangeAdapter changeAdapter : versions.get(version)) {
                                    final Name changeName = changeAdapter.getAnnotation().getName();
                                    if (toRemove.equals(changeName.getLocalName())) {
                                        for (Anchor anchor : changeAdapter.getAnnotation().getAnchors()) {
                                            rangesToRemove.add(anchor.getRange());
                                        }
                                    }
                                }
                            }

                            this.version++;
                            return TextRanges.compress(rangesToRemove);
                        }
                    };
                }
            };

            int version = 0;
            for (final SortedSet<TextRange> rangesToRemove : removedRanges) {
                final BufferedWriter out = Files.newWriter(new File(OUTPUT_FILE, testFile.getName().replaceAll("\\.xml$", "-" + (version++) + ".txt")), Charset.forName("UTF-8"));
                try {
                    new QueryResultTextStream<KeyValues>(repository, testLayer, Query.none()).stream(new TextStream.ListenerAdapter<KeyValues>() {
                        @Override
                        public void text(TextRange r, String text) {
                            int removed = 0;
                            StringBuffer buf = new StringBuffer(text);
                            for (Iterator<TextRange> it = rangesToRemove.iterator(); it.hasNext(); ) {
                                final TextRange rangeToRemove = it.next();
                                if (rangeToRemove.precedes(r)) {
                                    it.remove();
                                    continue;
                                } else if (rangeToRemove.follows(r)) {
                                    break;
                                }

                                final TextRange overlap = rangeToRemove.overlap(r);
                                final TextRange shifted = overlap.shift(-(r.getStart() + removed));
                                buf.replace((int) shifted.getStart(), (int) shifted.getEnd(), "");
                                removed += overlap.length();
                            }
                            try {
                                out.write(buf.toString());
                            } catch (IOException e) {
                                throw Throwables.propagate(e);
                            }
                        }
                    });
                } finally {
                    Closeables.close(out, false);
                }
            }
        }
    }

    void delete(File directory) {
        if (directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                if (file.isDirectory()) {
                    delete(file);
                }
                file.delete();
            }
        }
        directory.delete();
    }

    private static final TextStream.ListenerAdapter PRINTING_TEXT_LISTENER = new TextStream.ListenerAdapter() {

        @Override
        public void start(long contentLength) {
            System.out.println(Strings.repeat("=", 100));
        }

        @Override
        public void end() {
            System.out.println();
            System.out.println(Strings.repeat("=", 100));
        }

        @Override
        public void text(TextRange r, String text) {
            System.out.print(text);
        }
    };
}
