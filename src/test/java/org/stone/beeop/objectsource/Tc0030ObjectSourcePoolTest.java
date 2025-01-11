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
import org.stone.beeop.BeeObjectFactory;
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.objects.JavaBookFactory;

import static org.stone.beeop.config.OsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0030ObjectSourcePoolTest extends TestCase {

    public void testOnConfig() {
        try {
            new BeeObjectSource(createDefault());
        } catch (Exception e) {
            fail("test failed on testOnConfig");
        }
    }

    public void testPoolCreateFailed() {
        BeeObjectSourceConfig config = createDefault();
        config.setMaxActive(10);
        config.setInitialSize(50);
        try {
            new BeeObjectSource(config);//check fail
        } catch (RuntimeException e) {
            Assert.assertEquals("initial-size must not be greater than max-active", e.getMessage());
        }

        try {
            BeeObjectSourceConfig config2 = createDefault();
            config2.setPoolImplementClassName("xx.xx.xx");//invalid pool class name
            new BeeObjectSource(config2);
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();
            Assert.assertTrue(cause instanceof ClassNotFoundException);
        }
    }

    public void testPoolNotReady() {
        BeeObjectSource os = new BeeObjectSource();
        try {
            os.getObjectCreatingCount(null);
        } catch (Exception e) {
            Assert.assertEquals("Pool not be created", e.getMessage());
        }
        try {
            os.getObjectCreatingTimeoutCount(null);
        } catch (Exception e) {
            Assert.assertEquals("Pool not be created", e.getMessage());
        }
        try {
            os.interruptObjectCreating(null, true);
        } catch (Exception e) {
            Assert.assertEquals("Pool not be created", e.getMessage());
        }

        try {
            os.getPoolMonitorVo();
        } catch (Exception e) {
            Assert.assertEquals("Pool not be created", e.getMessage());
        }

        try {
            os.getMonitorVo(null);
        } catch (Exception e) {
            Assert.assertEquals("Pool not be created", e.getMessage());
        }

        try {
            os.setPrintRuntimeLog(null, false);
        } catch (Exception e) {
            Assert.assertEquals("Pool not be created", e.getMessage());
        }

        try {
            os.deleteKey(null);
        } catch (Exception e) {
            Assert.assertEquals("Pool not be created", e.getMessage());
        }

        try {
            os.deleteKey(null, true);
        } catch (Exception e) {
            Assert.assertEquals("Pool not be created", e.getMessage());
        }

        try {
            os.clear(true);
        } catch (Exception e) {
            Assert.assertEquals("Pool not be created", e.getMessage());
        }

        try {
            os.clear(true, new BeeObjectSourceConfig());
        } catch (Exception e) {
            Assert.assertEquals("Pool not be created", e.getMessage());
        }
    }

    public void testPoolReady() throws Exception {
        BeeObjectFactory factory = new JavaBookFactory();
        BeeObjectSourceConfig config2 = new BeeObjectSourceConfig();
        config2.setObjectFactory(factory);
        config2.setForceCloseUsingOnClear(true);
        BeeObjectSource os = new BeeObjectSource(config2);
        Object key = factory.getDefaultKey();

        os.getObjectCreatingCount(key);
        os.getObjectCreatingTimeoutCount(key);
        os.interruptObjectCreating(key, true);
        os.getPoolMonitorVo();
        os.getMonitorVo(key);
        os.setPrintRuntimeLog(key, false);
        os.deleteKey(key);
        os.deleteKey(key, true);
        os.clear(true);
    }

    public void testPoolLazyCreation() throws Exception {
        BeeObjectSource os = new BeeObjectSource();
        BeeObjectFactory factory = new JavaBookFactory();

        //1:before setting factory
        try {
            os.getObjectHandle();
        } catch (Exception e) {
            Assert.assertEquals("Must provide one of config items[objectFactory,objectClassName,objectFactoryClassName]", e.getMessage());
        }

        try {
            os.getObjectHandle(factory.getDefaultKey());
        } catch (Exception e) {
            Assert.assertEquals("Must provide one of config items[objectFactory,objectClassName,objectFactoryClassName]", e.getMessage());
        }

        //1:after setting factory
        os.setObjectFactory(factory);
        os.setForceCloseUsingOnClear(true);
        os.setMaxActive(2);
        Assert.assertNotNull(os.getObjectHandle());
        Assert.assertNotNull(os.getObjectHandle(factory.getDefaultKey()));
    }
}
