package console;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import vavi.util.Debug;


/**
 * Test001.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-07-05 nsano initial version <br>
 */
class Test001 {

    static Path outDir;

    @BeforeAll
    static void setup() throws IOException {
        outDir = Path.of("tmp/out");
        if (!Files.exists(outDir)) {
            Files.createDirectories(outDir);
        }
    }

    @Test
    void test1() throws Exception {
        Files.list(Path.of("src/test/resources/samples/")).forEach(p -> {
            Path out = outDir.resolve(Program.getCompledFilename(p));
            Program.main(new String[] {
                    p.toString(),
                    out.toString()
            });
            if (!Files.exists(out))
                Debug.println(Level.WARNING, out.toString());
        });
    }
}
