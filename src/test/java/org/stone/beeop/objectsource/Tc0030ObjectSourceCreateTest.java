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
import org.stone.beeop.BeeObjectSourceConfig;

import static org.stone.beeop.config.OsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */

public class Tc0030ObjectSourceCreateTest extends TestCase {

    public void testOnConfig() {
        try {
            new BeeObjectSource(createDefault());
        } catch (Exception e) {
            fail("test failed on testOnConfig");
        }
    }

    public void testPoolClassNotFound() {
        BeeObjectSource ds = null;
        try {
            BeeObjectSourceConfig config = createDefault();
            config.setPoolImplementClassName("xx.xx.xx");//invalid pool class name
            ds = new BeeObjectSource(config);
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();
            Assert.assertTrue(cause instanceof ClassNotFoundException);
        } finally {
            if (ds != null) ds.close();
        }
    }
}
