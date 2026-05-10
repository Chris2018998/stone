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
import org.stone.beeop.BeeObjectFactory;
import org.stone.beeop.BeeObjectHandle;
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.exception.BeePooledObjectKeyException;
import org.stone.test.beeop.config.OsConfigFactory;
import org.stone.test.beeop.objects.book.Book;

/**
 * @author Chris Liao
 */
public class Tc0062PooledBucketSuspensionTest {

    @Test
    public void testSuspendBucket() throws Exception {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        BeeObjectFactory<String, Book> bookFactory = config.getObjectFactory();

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            //1: suspend bucket
            os.suspendBucket(bookFactory.getDefaultKey());
            Assertions.assertTrue(os.getBucketMonitorVo(bookFactory.getDefaultKey()).isSuspended());
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle()) {
                Assertions.fail("[Tc0062PooledBucketSuspensionTest.testSuspendKey]failed");
            } catch (Exception e) {
                Assertions.assertInstanceOf(BeePooledObjectKeyException.class, e);
                Assertions.assertEquals("Object bucket was not ready", e.getMessage());
            }

            //2: resume bucket
            os.resumeBucket(bookFactory.getDefaultKey());
            Assertions.assertFalse(os.getBucketMonitorVo(bookFactory.getDefaultKey()).isSuspended());
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle()) {
                Assertions.assertNotNull(ignored);
            } catch (Exception e) {
                Assertions.fail("[Tc0062PooledBucketSuspensionTest.testSuspendKey]failed");
            }
        }
    }
}



