package console;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vavix.util.Checksum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;


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

    /** .muc files */
    static Stream<Arguments> sources() throws IOException {
        return Files.list(Path.of("src/test/resources/samples/")).filter(p -> p.toString().endsWith(".muc")).map(p -> arguments(p));
    }

    /** compile .muc at "resource/samples" to .mub into "tmp/out" */
    @ParameterizedTest
    @MethodSource("sources")
    void test1(Path p) throws Exception {
        Path out = outDir.resolve(Program.getCompledFilename(p));
        Program.main(new String[] {
                p.toString(),
                out.toString()
        });
        assertTrue(Files.exists(out), out.toString());
        Path excpected = p.getParent().resolve(Program.getCompledFilename(p));
        if (Files.exists(excpected)) {
            assertEquals(Checksum.getChecksum(excpected), Checksum.getChecksum(out));
        }
    }

    /** .mub files */
    static Stream<Arguments> sources2() throws IOException {
        return Files.list(outDir).filter(p -> p.toString().endsWith(".mub")).map(p -> arguments(p));
    }

    /** play .mub at "tmp/out" */
    @ParameterizedTest
    @MethodSource("sources2")
    void test2(Path p) throws Exception {
        Path out = outDir.resolve(Program.getCompledFilename(p));
        mucom88.player.Program.main(new String[] {p.toString()});
    }
}
