/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beeop.pool;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.pool.ObjectPool;

import static org.stone.test.beeop.config.OsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0051KeyPoolInitTest {

    @Test
    public void testNullConfig() {
        ObjectPool pool = new ObjectPool();
        try {
            pool.start(null);
        } catch (Exception e) {
            Assertions.assertEquals("Object source configuration can't be null", e.getMessage());
        }
    }

    @Test
    public void testCasInitialize() throws Exception {
        ObjectPool pool = new ObjectPool();
        BeeObjectSourceConfig config = createDefault();

        InitializeThread thread1 = new InitializeThread(pool, config);
        InitializeThread thread2 = new InitializeThread(pool, config);
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        if (thread1.getFailureException() != null) {
            Assertions.assertEquals("Object pool has initialized or in initializing", thread1.getFailureException().getMessage());
        }

        if (thread2.getFailureException() != null) {
            Assertions.assertEquals("Object pool has initialized or in initializing", thread2.getFailureException().getMessage());
        }
    }

    private static class InitializeThread extends Thread {
        private final ObjectPool pool;
        private final BeeObjectSourceConfig config;
        private Exception failureException;

        public InitializeThread(ObjectPool pool, BeeObjectSourceConfig config) {
            this.pool = pool;
            this.config = config;
        }

        public void run() {
            try {
                pool.start(config);
            } catch (Exception e) {
                this.failureException = e;
            }
        }

        public Exception getFailureException() {
            return failureException;
        }
    }
}
