/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.config;

import org.junit.jupiter.api.Test;
import org.stone.beecp.pool.ConnectionPoolStatics;
import org.stone.test.base.TestUtil;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;
import static org.stone.test.base.TestUtil.invokeMethod2;

/**
 * Proxy classes check test
 *
 * @author Chris Liao
 */
public class Tc0001ProxyClassTest {

    @Test
    public void testCheckJdbcProxyClasses() throws Exception {
        String conBorrowerClassFileName = "org/stone/beecp/pool/Borrower.class";
        File conBorrowerClassFile = TestUtil.getClassPathFileAbsolutePath(conBorrowerClassFileName);
        assert conBorrowerClassFile != null;

        String conBorrowerClassFileFullName = conBorrowerClassFile.toString();
        int pos = conBorrowerClassFileFullName.lastIndexOf(File.separator);
        String folderName = conBorrowerClassFileFullName.substring(0, pos);

        //create a new file for copy content from conBorrowerClassFile
        File conBorrowerClassFile2 = new File(folderName + File.separator + "Borrower2.class");
        try {
            //run pool classes check
            assertTrue(conBorrowerClassFile.renameTo(conBorrowerClassFile2));
            invokeMethod2(null, ConnectionPoolStatics.class, "checkJdbcProxyClass");
            fail("[testCheckJdbcProxyClasses]Not thrown exception when proxy classes missed");
        } catch (InvocationTargetException e) {
            assertInstanceOf(ClassNotFoundException.class, e.getCause());//class not found exception is expected
        } finally {
            assertTrue(conBorrowerClassFile2.renameTo(conBorrowerClassFile));//class file restore
        }
    }
}
