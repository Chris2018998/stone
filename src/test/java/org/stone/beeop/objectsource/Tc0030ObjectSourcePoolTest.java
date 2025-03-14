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
import org.stone.beeop.BeeObjectHandle;
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
            Assert.assertEquals("The configured value of item 'initial-size' cannot be greater than the configured value of item 'max-active'", e.getMessage());
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
            os.interruptObjectCreating(null, true);
        } catch (Exception e) {
            Assert.assertEquals("Pool not be created", e.getMessage());
        }

        try {
            os.getPoolMonitorVo();
        } catch (Exception e) {
            Assert.assertEquals("Pool not be created", e.getMessage());
        }

//        try {
//            os.keys();
//        } catch (Exception e) {
//            Assert.assertEquals("Pool not be created", e.getMessage());
//        }

        try {
            os.exists(null);
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
            os.clear(null);
        } catch (Exception e) {
            Assert.assertEquals("Pool not be created", e.getMessage());
        }

        try {
            os.clear(null, true);
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
        config2.setForceRecycleBorrowedOnClose(true);
        BeeObjectSource os = new BeeObjectSource(config2);
        Object key = factory.getDefaultKey();

        Assert.assertTrue(os.exists(key));
//        Assert.assertEquals(1, os.keys().length);
        os.interruptObjectCreating(key, true);
        os.getPoolMonitorVo();
        os.getMonitorVo(key);
        os.setPrintRuntimeLog(key, false);
        os.clear(true);
        Assert.assertTrue(os.exists(key));//<--default key forbidden to delete
        os.clear(key);
        Assert.assertTrue(os.exists(key));//<--default key forbidden to delete
        os.clear(key, true);
        Assert.assertTrue(os.exists(key));//<--default key forbidden to delete
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
        os.setForceRecycleBorrowedOnClose(true);
        os.setMaxActive(2);
        Assert.assertNotNull(os.getObjectHandle());
        Assert.assertNotNull(os.getObjectHandle(factory.getDefaultKey()));
    }

    public void testKeyDelete() throws Exception {
        BeeObjectSource os = new BeeObjectSource();
        BeeObjectFactory factory = new JavaBookFactory();
        os.setParkTimeForRetry(0L);
        os.setObjectFactory(factory);
        os.setForceRecycleBorrowedOnClose(true);
        os.setMaxActive(2);

        Object defaultKey = factory.getDefaultKey();
        Object key1 = new Object();
        Assert.assertNotNull(os.getObjectHandle());
        Assert.assertNotNull(os.getObjectHandle(key1));

        Assert.assertTrue(os.exists(defaultKey));
        Assert.assertTrue(os.exists(key1));

        os.deleteKey(key1, true);
        Assert.assertFalse(os.exists(key1));

        BeeObjectHandle handle = os.getObjectHandle(key1);
        Assert.assertNotNull(handle);
        handle.close();
        Assert.assertTrue(os.exists(key1));
        os.deleteKey(key1);
        Assert.assertFalse(os.exists(key1));

        try {
            os.deleteKey(defaultKey);
        } catch (Exception e) {
            Assert.assertEquals("Default key is forbidden to delete", e.getMessage());
        }
        Assert.assertTrue(os.exists(defaultKey));//<--default key forbidden to delete
        try {
            os.deleteKey(defaultKey, true);
        } catch (Exception e) {
            Assert.assertEquals("Default key is forbidden to delete", e.getMessage());
        }
        Assert.assertTrue(os.exists(defaultKey));//<--default key forbidden to delete
    }
}
