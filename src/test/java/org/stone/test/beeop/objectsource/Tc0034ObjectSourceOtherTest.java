/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beeop.objectsource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beeop.BeeObjectSource;
import org.stone.test.beeop.objects.JavaBookFactory;

import java.security.InvalidParameterException;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Chris Liao
 */
public class Tc0034ObjectSourceOtherTest {

    @Test
    public void testSetMaxWait() {
        long maxWait = 8000L;
        BeeObjectSource os = new BeeObjectSource();
        Assertions.assertEquals(maxWait, os.getMaxWait());
        try {
            os.setMaxWait(-1L);
            fail("Setting test failed on configuration item[max-wait]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'max-wait' must be greater than zero", e.getMessage());
        }
        try {
            os.setMaxWait(0L);
            fail("Setting test failed on configuration item[max-wait]");
        } catch (InvalidParameterException e) {
            Assertions.assertEquals("The given value for configuration item 'max-wait' must be greater than zero", e.getMessage());
        }
        os.setMaxWait(10L);
        Assertions.assertEquals(10L, os.getMaxWait());
    }

    @Test
    public void testClose() throws Exception {
        BeeObjectSource os = new BeeObjectSource();
        Assertions.assertTrue(os.isClosed());
        os.close();
        Assertions.assertTrue(os.isClosed());

        os.setForceRecycleBorrowedOnClose(true);
        Assertions.assertTrue(os.isClosed());
        os.setObjectFactory(new JavaBookFactory());
        os.getObjectHandle();
        Assertions.assertFalse(os.isClosed());
        os.close();
        Assertions.assertTrue(os.isClosed());
    }

    @Test
    public void testSetPrintRuntimeLog() throws Exception {
        BeeObjectSource os = new BeeObjectSource();
        os.setForceRecycleBorrowedOnClose(true);
        JavaBookFactory objectFactory = new JavaBookFactory();
        os.setObjectFactory(new JavaBookFactory());
        os.setPrintRuntimeLogs(true);
        try {
            os.isEnabledLogPrint(objectFactory.getDefaultKey());
        } catch (Exception e) {
            Assertions.assertEquals("Pool not be created", e.getMessage());
        }

        //3: lazy initialization
        os.getObjectHandle(objectFactory.getDefaultKey());
        Assertions.assertTrue(os.isEnabledLogPrint(objectFactory.getDefaultKey()));

        os.setPrintRuntimeLogs(false);
        Assertions.assertFalse(os.isEnabledLogPrint(objectFactory.getDefaultKey()));
    }
}
