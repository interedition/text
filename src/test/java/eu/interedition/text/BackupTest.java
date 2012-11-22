package eu.interedition.text;

import com.google.common.io.Closeables;
import eu.interedition.text.util.BackupSupport;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import org.junit.Test;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class BackupTest extends AbstractTestResourceTest {

    @Test
    public void backup() throws IOException {
        if (repository instanceof BackupSupport) {
            text();

            final BackupSupport restorable = (BackupSupport) repository;
            final Charset charset = Charset.forName("UTF-8");

            final File backupFile = File.createTempFile(getClass() + ".backup", ".sql");
            backupFile.deleteOnExit();

            final Writer to = new OutputStreamWriter(new FileOutputStream(backupFile), charset);
            try {
                restorable.backup(to);
            } finally {
                Closeables.close(to, false);
            }

            // FIXME: restorable.restore(backupFile, charset);
        }
    }
}
