/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.shine;

import org.junit.Test;

/**
 * @author Chris.Liao
 * @version 1.0
 */
public class TestRunner {
    private static final String defaultFilename = "shine/testCase.properties";

    public static void main(String[] ags) throws Throwable {
        org.stone.base.TestRunner.main(new String[]{"shine", defaultFilename});
    }

    @Test
    public void testRun() throws Throwable {
        long beginTime = System.currentTimeMillis();
        org.stone.base.TestRunner.main(new String[]{"shine", defaultFilename});
        System.out.println("Took time:(" + (System.currentTimeMillis() - beginTime) + ")ms");
    }
}
