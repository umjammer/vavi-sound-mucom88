/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import mucom88.console.Program;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;
import vavix.util.Checksum;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * TestCase.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-07-05 nsano initial version <br>
 */
@EnabledIf("localPropertiesExists")
@PropsEntity(url = "file:local.properties")
public class TestCase {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "vavi.test.volume")
    double volume = 0.2;

    @Property
    String mub;
    @Property
    String muc;

    @Property
    String mucDir;
    @Property
    String mubDir;
    @Property
    String outDir;

    @Property
    String mucom;
    @Property
    String mucomDotNet;

    static ThreadLocal<Path> outPath = new ThreadLocal<>();
    static ThreadLocal<Path> mucDirPath = new ThreadLocal<>();
    static ThreadLocal<Path> mubDirPath = new ThreadLocal<>();

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }

        outPath.set(Path.of(outDir));
        if (!Files.exists(outPath.get())) {
            Files.createDirectories(outPath.get());
        }

        mucDirPath.set(Path.of(mucDir));
        mubDirPath.set(Path.of(mubDir));

        System.setProperty("mucom88.volume", String.valueOf(volume));
Debug.println("volume: " + System.getProperty("mucom88.volume"));
    }

    /** .muc (mml) files from sample dir */
    static Stream<Arguments> mucSources() throws IOException {
        return Files.walk(mucDirPath.get()).filter(p -> p.toString().endsWith(".muc")).map(Arguments::arguments);
    }

//    @Disabled("compiler not finished")
    @Test
    @DisplayName("compile .muc at dir to .mub")
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test0() throws Exception {
        Files.walk(mucDirPath.get())
                .filter(p -> p.toString().endsWith(".muc"))
                .forEach(p -> {
                    Path out = outPath.get().resolve(Program.getCompledFilename(p));
System.out.println(p + " -> " + out);
                    Program.main(new String[] {
                            p.toString(),
                            out.toString()
                    });
                });
    }

    @Disabled("it's compiled by extended mode")
    @ParameterizedTest
    @MethodSource("mucSources")
    @DisplayName("compile .muc at method source to .mub into specified dir")
    void test1(Path p) throws Exception {
        Path out = Path.of(outDir, Program.getCompledFilename(p));
System.out.println(p + " -> " + out);
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

    /** .mub (binary) files at out dir */
    static Stream<Arguments> mubSources() throws IOException {
        return Files.walk(mubDirPath.get()).filter(p -> p.toString().endsWith(".mub")).map(Arguments::arguments);
    }

    @ParameterizedTest
    @MethodSource("mubSources")
    @DisplayName("play .mub at method source")
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test2(Path p) throws Exception {
Debug.println(p);
        mucom88.player.Program.main(new String[] {p.toString()});
    }

    @Test
    @DisplayName("play .mub")
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test3() throws Exception {
Debug.println(mub);
        mucom88.player.Program.main(new String[] {mub});
    }

    @Test
    @DisplayName("compile & compare c# & play")
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    @Disabled("made compile same as the original native")
    void test4() throws Exception {
Debug.println(muc);
        Path testMUC = Path.of("tmp/test_java.muc");
        Path testMUB = Path.of("tmp/test_java.mub");
        Path testMUC2 = Path.of("tmp/test_dotnet.muc");
        Path testMUB2 = Path.of("tmp/test_dotnet.mub");

        Files.copy(Path.of(muc), testMUC, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Path.of(muc), testMUC2, StandardCopyOption.REPLACE_EXISTING);
        Files.deleteIfExists(testMUB);
        Files.deleteIfExists(testMUB2);

Debug.println("c# ----------------------------------------------------------------");
        // compile c#
        ProcessBuilder pb = new ProcessBuilder();
        pb.inheritIO();
        Process p = pb.command(mucomDotNet, testMUC2.toRealPath().toString()).start();
        int r = p.waitFor();
        assertEquals(0, r);
        assertTrue(Files.exists(testMUB2), "c# compile failed");

Debug.println("java ----------------------------------------------------------------");
        // compile java
        mucom88.console.Program.main(new String[] {testMUC.toString()});
        assertTrue(Files.exists(testMUB), "java compile failed");

Debug.println("compare ----------------------------------------------------------------");
Debug.println("c#  : " + Files.size(testMUB2));
Debug.println("java: " + Files.size(testMUB));
        // compare
        assertEquals(Files.size(testMUB2), Files.size(testMUB), "java output is different from the original");

        // play
Debug.println("play mub created by java ----------------------------------------------------------------");
        mucom88.player.Program.main(new String[] {testMUB.toString()});
//Debug.println("play mub created by c# ----------------------------------------------------------------");
//        mucom88.player.Program.main(new String[] {testMUB2.toString()});
    }

    @Test
    @DisplayName("compile & compare native & play")
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test5() throws Exception {
Debug.println(muc);
        Path testMUC = Path.of("tmp/test_java.muc");
        Path testMUB = Path.of("tmp/test_java.mub");
        Path testMUC2 = Path.of("tmp/test_native.muc");
        Path testMUB2 = Path.of("tmp/test_native.mub");

        Files.copy(Path.of(muc), testMUC, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Path.of(muc), testMUC2, StandardCopyOption.REPLACE_EXISTING);
        Files.deleteIfExists(testMUB);
        Files.deleteIfExists(testMUB2);

Debug.println("native ----------------------------------------------------------------");
        // compile native
        ProcessBuilder pb = new ProcessBuilder();
        pb.inheritIO();
        Process p = pb.command(mucom, "-g", testMUC2.toRealPath().toString(), "-o", testMUB2.toString()).start();
        int r = p.waitFor();
        assertEquals(0, r);
        assertTrue(Files.exists(testMUB2), "native compile failed");

Debug.println("java ----------------------------------------------------------------");
        // compile java
        mucom88.console.Program.main(new String[] {testMUC.toString()});
        assertTrue(Files.exists(testMUB), "java compile failed");

Debug.println("compare ----------------------------------------------------------------");
Debug.println("native: " + Files.size(testMUB2));
Debug.println("java  : " + Files.size(testMUB));
        // compare
        assertEquals(Files.size(testMUB2), Files.size(testMUB), "java output is different from the original");

        // play
//Debug.println("play mub created by java ----------------------------------------------------------------");
//        mucom88.player.Program.main(new String[] {testMUB.toString()});
Debug.println("play mub created by native ----------------------------------------------------------------");
        mucom88.player.Program.main(new String[] {testMUB2.toString()});
    }

    @Test
    @DisplayName("compile .muc")
    void test6() throws Exception {
Debug.println(muc);
        mucom88.console.Program.main(new String[] {muc});
    }
}
