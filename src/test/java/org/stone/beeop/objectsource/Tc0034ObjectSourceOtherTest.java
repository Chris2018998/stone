/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beeop.objectsource;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.objects.JavaBookFactory;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author Chris Liao
 */
public class Tc0034ObjectSourceOtherTest extends TestCase {

    public void testSetMaxWait() {
        long maxWait = SECONDS.toMillis(8L);
        BeeObjectSource os = new BeeObjectSource();
        Assert.assertEquals(maxWait, os.getMaxWait());
        os.setMaxWait(0L);
        Assert.assertEquals(maxWait, os.getMaxWait());
        os.setMaxWait(10L);
        Assert.assertEquals(10L, os.getMaxWait());
    }

    public void testClose() throws Exception {
        BeeObjectSource os = new BeeObjectSource();
        Assert.assertTrue(os.isClosed());
        os.close();
        Assert.assertTrue(os.isClosed());

        os.setForceCloseUsingOnClear(true);
        Assert.assertTrue(os.isClosed());
        os.setObjectFactory(new JavaBookFactory());
        os.getObjectHandle();
        Assert.assertFalse(os.isClosed());
        os.close();
        Assert.assertTrue(os.isClosed());
    }

    public void testSetPrintRuntimeLog() throws Exception {
        BeeObjectSource os = new BeeObjectSource();
        os.setForceCloseUsingOnClear(true);
        JavaBookFactory objectFactory = new JavaBookFactory();
        os.setObjectFactory(new JavaBookFactory());
        os.setPrintRuntimeLog(true);
        try {
            os.isPrintRuntimeLog(objectFactory.getDefaultKey());
        } catch (Exception e) {
            Assert.assertEquals("Pool not be created", e.getMessage());
        }

        //3: lazy initialization
        os.getObjectHandle(objectFactory.getDefaultKey());
        Assert.assertTrue(os.isPrintRuntimeLog(objectFactory.getDefaultKey()));

        os.setPrintRuntimeLog(false);
        Assert.assertFalse(os.isPrintRuntimeLog(objectFactory.getDefaultKey()));
    }
}
