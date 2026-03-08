/// *
// * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
// *
// * Copyright(C) Chris2018998,All rights reserved.
// *
// * Project owner contact:Chris2018998@tom.com.
// *
// * Project Licensed under Apache License v2.0
// */
//package org.stone.test.beeop.objectsource;
//
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.Test;
//import org.stone.beeop.BeeObjectFactory;
//import org.stone.beeop.BeeObjectHandle;
//import org.stone.beeop.BeeObjectSource;
//import org.stone.beeop.BeeObjectSourceConfig;
//import org.stone.test.beeop.objects.JavaBookFactory;
//
//import static org.junit.jupiter.api.Assertions.fail;
//import static org.stone.test.beeop.config.OsConfigFactory.createDefault;
//
/// **
// * @author Chris Liao
// */
//public class Tc0030ObjectSourcePoolTest {
//
//    @Test
//    public void testOnConfig() {
//        try {
//            new BeeObjectSource(createDefault());
//        } catch (Exception e) {
//            fail("test failed on testOnConfig");
//        }
//    }
//
//    @Test
//    public void testPoolCreateFailed() {
//        BeeObjectSourceConfig config = createDefault();
//        config.setMaxActive(10);
//        config.setInitialSize(50);
//        try {
//            new BeeObjectSource(config);//check fail
//        } catch (RuntimeException e) {
//            Assertions.assertEquals("The configured value of item 'initial-size' cannot be greater than the configured value of item 'max-active'", e.getMessage());
//        }
//
//        try {
//            BeeObjectSourceConfig config2 = createDefault();
//            config2.setPoolImplementClassName("xx.xx.xx");//invalid pool class name
//            new BeeObjectSource(config2);
//        } catch (RuntimeException e) {
//            Throwable cause = e.getCause();
//            Assertions.assertInstanceOf(ClassNotFoundException.class, cause);
//        }
//    }
//
//    @Test
//    public void testPoolNotReady() {
//        BeeObjectSource os = new BeeObjectSource();
//        try {
//            os.interruptWaitingThreads(null);
//        } catch (Exception e) {
//            Assertions.assertEquals("Datasource pool not instantiated", e.getMessage());
//        }
//
/// /        try {
/// /            os.getPoolMonitorVo();
/// /        } catch (Exception e) {
/// /            Assertions.assertEquals("Datasource pool not instantiated", e.getMessage());
/// /        }
//
/// /        try {
/// /            os.keys();
/// /        } catch (Exception e) {
/// /            Assertions.assertEquals("Datasource pool not instantiated", e.getMessage());
/// /        }
//
//        try {
//            os.exists(null);
//        } catch (Exception e) {
//            Assertions.assertEquals("Datasource pool not instantiated", e.getMessage());
//        }
//
//        try {
//            os.getMonitorVo(null);
//        } catch (Exception e) {
//            Assertions.assertEquals("Datasource pool not instantiated", e.getMessage());
//        }
//
//        try {
//            os.enableLogPrint(null, false);
//        } catch (Exception e) {
//            Assertions.assertEquals("Datasource pool not instantiated", e.getMessage());
//        }
//
//        try {
//            os.clearObjects(null);
//        } catch (Exception e) {
//            Assertions.assertEquals("Datasource pool not instantiated", e.getMessage());
//        }
//
//        try {
//            os.clearObjects(null, true);
//        } catch (Exception e) {
//            Assertions.assertEquals("Datasource pool not instantiated", e.getMessage());
//        }
//
//        try {
//            os.deleteKey(null);
//        } catch (Exception e) {
//            Assertions.assertEquals("Datasource pool not instantiated", e.getMessage());
//        }
//
//        try {
//            os.deleteKey(null, true);
//        } catch (Exception e) {
//            Assertions.assertEquals("Datasource pool not instantiated", e.getMessage());
//        }
//
//        try {
//            os.restart(true);
//        } catch (Exception e) {
//            Assertions.assertEquals("Datasource pool not instantiated", e.getMessage());
//        }
//
//        try {
//            os.restart(true, new BeeObjectSourceConfig());
//        } catch (Exception e) {
//            Assertions.assertEquals("Datasource pool not instantiated", e.getMessage());
//        }
//    }
//
//    @Test
//    public void testPoolReady() throws Exception {
//        BeeObjectFactory factory = new JavaBookFactory();
//        BeeObjectSourceConfig config2 = new BeeObjectSourceConfig();
//        config2.setObjectFactory(factory);
//        config2.setForceRecycleBorrowedOnClose(true);
//        BeeObjectSource os = new BeeObjectSource(config2);
//        Object key = factory.getDefaultKey();
//
//        Assertions.assertTrue(os.exists(key));
/// /        Assertions.assertEquals(1, os.keys().length);
//        os.interruptWaitingThreads(key);
//        os.getPoolMonitorVo();
//        os.getMonitorVo(key);
//        os.enableLogPrint(key, false);
//        os.restart(true);
//        Assertions.assertTrue(os.exists(key));//<--default key forbidden to delete
//        os.clearObjects(key);
//        Assertions.assertTrue(os.exists(key));//<--default key forbidden to delete
//        os.clearObjects(key, true);
//        Assertions.assertTrue(os.exists(key));//<--default key forbidden to delete
//    }
//
//    @Test
//    public void testPoolLazyCreation() throws Exception {
//        BeeObjectSource os = new BeeObjectSource();
//        BeeObjectFactory factory = new JavaBookFactory();
//
//        //1:before setting factory
//        try {
//            os.getObjectHandle();
//        } catch (Exception e) {
//            Assertions.assertEquals("Must provide one of config items[objectFactory,objectClassName,objectFactoryClassName]", e.getMessage());
//        }
//
//        try {
//            os.getObjectHandle(factory.getDefaultKey());
//        } catch (Exception e) {
//            Assertions.assertEquals("Must provide one of config items[objectFactory,objectClassName,objectFactoryClassName]", e.getMessage());
//        }
//
//        //1:after setting factory
//        os.setObjectFactory(factory);
//        os.setForceRecycleBorrowedOnClose(true);
//        os.setMaxActive(2);
//        Assertions.assertNotNull(os.getObjectHandle());
//        Assertions.assertNotNull(os.getObjectHandle(factory.getDefaultKey()));
//    }
//
//    @Test
//    public void testKeyDelete() throws Exception {
//        BeeObjectSource os = new BeeObjectSource();
//        BeeObjectFactory factory = new JavaBookFactory();
//        os.setParkTimeForRetry(0L);
//        os.setObjectFactory(factory);
//        os.setForceRecycleBorrowedOnClose(true);
//        os.setMaxActive(2);
//
//        Object defaultKey = factory.getDefaultKey();
//        Object key1 = new Object();
//        Assertions.assertNotNull(os.getObjectHandle());
//        Assertions.assertNotNull(os.getObjectHandle(key1));
//
//        Assertions.assertTrue(os.exists(defaultKey));
//        Assertions.assertTrue(os.exists(key1));
//
//        os.deleteKey(key1, true);
//        Assertions.assertFalse(os.exists(key1));
//
//        BeeObjectHandle handle = os.getObjectHandle(key1);
//        Assertions.assertNotNull(handle);
//        handle.close();
//        Assertions.assertTrue(os.exists(key1));
//        os.deleteKey(key1);
//        Assertions.assertFalse(os.exists(key1));
//
//        try {
//            os.deleteKey(defaultKey);
//        } catch (Exception e) {
//            Assertions.assertEquals("Default key is forbidden to delete", e.getMessage());
//        }
//        Assertions.assertTrue(os.exists(defaultKey));//<--default key forbidden to delete
//        try {
//            os.deleteKey(defaultKey, true);
//        } catch (Exception e) {
//            Assertions.assertEquals("Default key is forbidden to delete", e.getMessage());
//        }
//        Assertions.assertTrue(os.exists(defaultKey));//<--default key forbidden to delete
//    }
//}
