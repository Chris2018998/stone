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
import org.stone.beeop.BeeObjectHandle;
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.test.base.TestUtil;
import org.stone.test.beeop.config.OsConfigFactory;
import org.stone.test.beeop.objects.book.Book;

/**
 * @author Chris Liao
 */
public class Tc0062PooledObjectAbortTest {

    @Test
    public void testAbort() throws Exception {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setInitialSize(1);
        config.setMaxActive(1);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Object object1 = null, object2 = null;
            BeeObjectHandle<String, Book> handle = null;
            try {
                handle = os.getObjectHandle();
                object1 = TestUtil.getFieldValue(handle, "instance");

                AbortThread thread1 = new AbortThread(handle);
                AbortThread thread2 = new AbortThread(handle);
                thread1.start();
                thread2.start();
                thread1.join();
                thread2.join();
                Assertions.assertNull(thread1.causeException);
                Assertions.assertNull(thread2.causeException);
                Assertions.assertTrue(handle.isClosed());
            } catch (Exception e) {
                Assertions.fail("[Tc0062PooledObjectAbortTest.testEviction]failed");
            } finally {
                if (handle != null && !handle.isClosed()) {
                    handle.close();
                }
            }

            try (BeeObjectHandle<String, Book> handle2 = os.getObjectHandle()) {
                object2 = TestUtil.getFieldValue(handle2, "instance");
            }
            Assertions.assertNotEquals(object1, object2);
        }
    }

    private static class AbortThread extends Thread {
        private final BeeObjectHandle<String, Book> handle;
        private Exception causeException;

        public AbortThread(BeeObjectHandle<String, Book> handle) {
            this.handle = handle;
        }

        public void run() {
            try {
                this.handle.abort();
            } catch (Exception e) {
                this.causeException = e;
            }
        }
    }
}
