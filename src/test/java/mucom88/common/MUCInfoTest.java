/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mucom88.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * MUCInfoTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-04-28 nsano initial version <br>
 */
class MUCInfoTest {

    @Test
    void test1() throws Exception {
        MUCInfo mucInfo = new MUCInfo();
        mucInfo.setSrcCPtr(100);
        int v = mucInfo.getAndIncSrcCPtr();
        assertEquals(100, v);
        v = mucInfo.getSrcCPtr();
        assertEquals(101, v);
        v = mucInfo.incAndGetSrcCPtr();
        assertEquals(102, v);
        v = mucInfo.getSrcCPtr();
        assertEquals(102, v);
    }
}
