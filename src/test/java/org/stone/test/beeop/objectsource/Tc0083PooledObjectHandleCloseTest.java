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
import org.stone.test.beeop.config.OsConfigFactory;
import org.stone.test.beeop.objects.book.Book;

/**
 * @author Chris Liao
 */
public class Tc0083PooledObjectHandleCloseTest {

    @Test
    public void testClose() throws Exception {
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(OsConfigFactory.createDefault())) {
            BeeObjectHandle<String, Book> handle = null;
            try {
                handle = os.getObjectHandle();
                CloseThread thread1 = new CloseThread(handle);
                CloseThread thread2 = new CloseThread(handle);
                thread1.start();
                thread2.start();
                thread1.join();
                thread2.join();
                Assertions.assertNull(thread1.causeException);
                Assertions.assertNull(thread2.causeException);
                Assertions.assertTrue(handle.isClosed());

                try {
                    handle.getKey();
                } catch (Exception e) {
                    Assertions.assertEquals("No operations allowed after object handle closed", e.getMessage());
                }
            } catch (Exception e) {
                Assertions.fail("[Tc0083PooledObjectHandleCloseTest.testEviction]failed");
            } finally {
                if (handle != null && !handle.isClosed()) {
                    handle.close();
                }
            }
        }
    }

    private static class CloseThread extends Thread {
        private final BeeObjectHandle<String, Book> handle;
        private Exception causeException;

        public CloseThread(BeeObjectHandle<String, Book> handle) {
            this.handle = handle;
        }

        public void run() {
            try {
                this.handle.close();
            } catch (Exception e) {
                this.causeException = e;
            }
        }
    }
}
