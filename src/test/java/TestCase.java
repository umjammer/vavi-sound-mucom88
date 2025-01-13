/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import console.Program;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;
import vavix.util.Checksum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * TestCase.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-07-05 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
public class TestCase {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "vavi.test.volume")
    double volume = 0.2;

    @Property
    String file;

    static Path outDir;

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }

Debug.println("volume: " + volume);
    }

    @BeforeAll
    static void setupAll() throws IOException {
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

    @Test
    @DisplayName("compile .muc at dir to .mub")
    void test0() throws Exception {
        Path dir = Path.of("tmp/iwamoo_mucom88muc_2018-2022/");
        Files.list(dir)
                .filter(p -> p.toString().endsWith(".muc"))
                .forEach(p -> {
                    Path out = dir.resolve(Program.getCompledFilename(p));
                    Program.main(new String[] {
                            p.toString(),
                            out.toString()
                    });
                });
    }

    @Disabled("it's compiled by extended mode")
    @ParameterizedTest
    @MethodSource("sources")
    @DisplayName("compile .muc at method source to .mub into tmp/out")
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

    @ParameterizedTest
    @MethodSource("sources22")
    @DisplayName("play .mub at method source")
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test2(Path p) throws Exception {
        mucom88.player.Program.main(new String[] {p.toString()});
    }

    @Test
    @DisplayName("play .mub")
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test3() throws Exception {
        mucom88.player.Program.main(new String[] {file});
    }
}
