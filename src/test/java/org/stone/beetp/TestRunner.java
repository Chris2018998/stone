/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.stone.beetp;

public class TestRunner {
    private static final String defaultFilename = "beetp/testCase.properties";

    public static void main(String[] ags) throws Throwable {
        org.stone.base.TestRunner.main(new String[]{"beetp", defaultFilename});
    }

    public void testRun() throws Throwable {
        long beginTime = System.currentTimeMillis();
        org.stone.base.TestRunner.main(new String[]{"beetp", defaultFilename});
        System.out.println("Took time:(" + (System.currentTimeMillis() - beginTime) + ")ms");
    }
}