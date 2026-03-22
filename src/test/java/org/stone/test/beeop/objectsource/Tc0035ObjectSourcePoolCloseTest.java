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
import org.stone.test.beeop.config.OsConfigFactory;
import org.stone.test.beeop.objects.book.Book;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author Chris Liao
 */
public class Tc0035ObjectSourcePoolCloseTest {

    @Test
    public void test() throws Exception {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        BeeObjectSource<String, Book> os = new BeeObjectSource<>(config);
        Assertions.assertFalse(os.isClosed());

        long runTime = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(500L);
        CloseThread close1 = new CloseThread(runTime, os);
        CloseThread close2 = new CloseThread(runTime, os);
        close1.start();
        close2.start();
        close1.join();
        close2.join();
        Assertions.assertTrue(os.isClosed());
    }

    private static class CloseThread extends Thread {
        private final long runTime;
        private final BeeObjectSource<String, Book> os;

        public CloseThread(long runTime, BeeObjectSource<String, Book> os) {
            this.os = os;
            this.runTime = runTime;
        }

        public void run() {
            LockSupport.parkNanos(runTime - System.nanoTime());
            os.close();
        }
    }
}
