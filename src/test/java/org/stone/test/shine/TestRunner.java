/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.test.shine;

/**
 * @author Chris.Liao
 * @version 1.0
 */
public class TestRunner {
    private static final String defaultFilename = "file/shine/testCase.properties";

    public static void main(String[] ags) throws Throwable {
        org.stone.test.base.TestRunner.main(new String[]{"file/shine", defaultFilename});
    }

    public void testRun() throws Throwable {
        long beginTime = System.currentTimeMillis();
        org.stone.test.base.TestRunner.main(new String[]{"file/shine", defaultFilename});
        System.out.println("Took time:(" + (System.currentTimeMillis() - beginTime) + ")ms");
    }
}
