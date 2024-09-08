/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import console.Program;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vavix.util.Checksum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


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

    /** .muc files from sample dir */
    static Stream<Arguments> sources() throws IOException {
        return Files.list(Path.of("src/test/resources/samples/")).filter(p -> p.toString().endsWith(".muc")).map(Arguments::arguments);
    }

    /** .muc files from test dir */
    static Stream<Arguments> sources1() throws IOException {
        return Files.list(Path.of("src/test/resources/test/")).filter(p -> p.toString().endsWith(".muc")).map(Arguments::arguments);
    }

    /** compile .muc at method source to .mub into "tmp/out" */
    @ParameterizedTest
    @MethodSource("sources")
    void test1(Path p) throws Exception {
        Path out = outDir.resolve(Program.getCompledFilename(p));
        Program.main(new String[] {
                p.toString(),
                out.toString()
        });
        assertTrue(Files.exists(out), out.toString());
        Path expected = p.getParent().resolve(Program.getCompledFilename(p));
        if (Files.exists(expected)) { // TODO only one .mub exists, and compiled by extended mode
            assertEquals(Checksum.getChecksum(expected), Checksum.getChecksum(out));
        }
    }

    /** .mub files at out dir */
    static Stream<Arguments> sources2() throws IOException {
        return Files.list(outDir).filter(p -> p.toString().endsWith(".mub")).map(Arguments::arguments);
    }

    /** .mub files at test resources */
    static Stream<Arguments> sources22() throws IOException {
        return Files.list(Path.of("src/test/resources/test/")).filter(p -> p.toString().endsWith(".mub")).map(Arguments::arguments);
    }

    /** play .mub at method source */
    @ParameterizedTest
    @MethodSource("sources22")
    void test2(Path p) throws Exception {
        mucom88.player.Program.main(new String[] {p.toString()});
    }
}
