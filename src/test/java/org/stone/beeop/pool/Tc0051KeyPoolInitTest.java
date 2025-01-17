/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beeop.pool;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beeop.BeeObjectSourceConfig;

import static org.stone.beeop.config.OsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0051KeyPoolInitTest extends TestCase {

    public void testNullConfig() {
        KeyedObjectPool pool = new KeyedObjectPool();
        try {
            pool.init(null);
        } catch (Exception e) {
            Assert.assertEquals("Object pool configuration can't be null", e.getMessage());
        }
    }

    public void testCasInitialize() throws Exception {
        KeyedObjectPool pool = new KeyedObjectPool();
        BeeObjectSourceConfig config = createDefault();

        InitializeThread thread1 = new InitializeThread(pool, config);
        InitializeThread thread2 = new InitializeThread(pool, config);
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        if (thread1.getFailureException() != null) {
            Assert.assertEquals("Object keyed pool has initialized or in initializing", thread1.getFailureException().getMessage());
        }

        if (thread2.getFailureException() != null) {
            Assert.assertEquals("Object keyed pool has initialized or in initializing", thread2.getFailureException().getMessage());
        }
    }

    private static class InitializeThread extends Thread {
        private final KeyedObjectPool pool;
        private final BeeObjectSourceConfig config;
        private Exception failureException;

        public InitializeThread(KeyedObjectPool pool, BeeObjectSourceConfig config) {
            this.pool = pool;
            this.config = config;
        }

        public void run() {
            try {
                pool.init(config);
            } catch (Exception e) {
                this.failureException = e;
            }
        }

        public Exception getFailureException() {
            return failureException;
        }
    }
}
