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
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.test.base.TestUtil;
import org.stone.test.beeop.config.OsConfigFactory;
import org.stone.test.beeop.objects.book.Book;

/**
 * @author Chris Liao
 */
public class Tc0033ObjectSourcePoolJvmHookTest {

    @Test
    public void testRegistration() throws Exception {
        //1: Register
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setRegisterJvmHook(true);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Object pool = TestUtil.getFieldValue(os, "pool");
            Assertions.assertNotNull(pool);
            Assertions.assertNotNull(TestUtil.getFieldValue(pool, "jvmExitHook"));
        }

        //2: Not Register
        config.setRegisterJvmHook(false);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Object pool = TestUtil.getFieldValue(os, "pool");
            Assertions.assertNotNull(pool);
            Assertions.assertNull(TestUtil.getFieldValue(pool, "jvmExitHook"));
        }
    }

    @Test
    public void testOnRestart() throws Exception {
        //Register ----> Not  Register
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setRegisterJvmHook(true);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Object pool = TestUtil.getFieldValue(os, "pool");
            Assertions.assertNotNull(pool);
            Assertions.assertNotNull(TestUtil.getFieldValue(pool, "jvmExitHook"));

            config.setRegisterJvmHook(false);
            os.restart(true, config);
            Assertions.assertNotNull(pool);
            Assertions.assertNull(TestUtil.getFieldValue(pool, "jvmExitHook"));
        }

        //Not  Register  ---> Register
        BeeObjectSourceConfig<String, Book> config2 = OsConfigFactory.createDefault();
        config2.setRegisterJvmHook(false);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config2)) {
            Object pool = TestUtil.getFieldValue(os, "pool");
            Assertions.assertNotNull(pool);
            Assertions.assertNull(TestUtil.getFieldValue(pool, "jvmExitHook"));

            config2.setRegisterJvmHook(true);
            os.restart(true, config2);
            Assertions.assertNotNull(pool);
            Assertions.assertNotNull(TestUtil.getFieldValue(pool, "jvmExitHook"));
        }
    }

//    @Test
//    public void testJvmExit() throws Exception {
//        //1: Register
//        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
//        config.setRegisterJvmHook(true);
//        config.setPrintRuntimeLogs(true);
//        BeeObjectSource<String, Book> os = new BeeObjectSource<>(config);
//        System.exit(0);
//    }
}
