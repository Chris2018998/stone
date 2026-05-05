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

import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * @author Chris Liao
 */
public class Tc0066PooledBucketThreadLocalTest {

    @Test
    public void testUseThreadLocal() throws Exception {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setUseThreadLocal(true);
        config.setInitialSize(1);
        config.setMaxActive(1);

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Object pool = TestUtil.getFieldValue(os, "pool");
            Map<String, ?> objectBucketMap = (Map<String, ?>) TestUtil.getFieldValue(pool, "objectBucketMap");
            Object objectBucket = objectBucketMap.get(config.getObjectFactory().getDefaultKey());
            ThreadLocal<WeakReference<Object>> threadLocal = (ThreadLocal<WeakReference<Object>>) TestUtil.getFieldValue(objectBucket, "threadLocal");

            //1: no cached object
            Assertions.assertNotNull(threadLocal.get().get());
            Object lastUsedPooledObject = TestUtil.getFieldValue(threadLocal.get().get(), "lastUsed");
            Assertions.assertNull(lastUsedPooledObject);//no cached pooled object in ThreadLocal

            //2: get a pooled object
            Object objectInstance1;
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle()) {
                Object borrower1 = threadLocal.get().get();
                Object pooledObject1 = TestUtil.getFieldValue(borrower1, "lastUsed");
                objectInstance1 = TestUtil.getFieldValue(pooledObject1, "objectInstance");
                borrower1 = null;//must set null before gc
            }

            //3: get again
            Object objectInstance2;
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle()) {
                Object borrower2 = threadLocal.get().get();
                Object pooledObject2 = TestUtil.getFieldValue(borrower2, "lastUsed");
                objectInstance2 = TestUtil.getFieldValue(pooledObject2, "objectInstance");
                Assertions.assertSame(objectInstance1, objectInstance2);
                borrower2 = null;//must set null before gc
            }

            //gc test
            System.gc();
            Assertions.assertNull(threadLocal.get().get());//gc
            try (BeeObjectHandle<String, Book> ignored = os.getObjectHandle()) {
                Assertions.assertNotNull(ignored);
            }
        }
    }

    @Test
    public void testDisableThreadLocal() throws Exception {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setUseThreadLocal(false);
        config.setInitialSize(1);
        config.setMaxActive(1);

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Object pool = TestUtil.getFieldValue(os, "pool");
            Map<String, ?> objectBucketMap = (Map<String, ?>) TestUtil.getFieldValue(pool, "objectBucketMap");
            Object objectBucket = objectBucketMap.get(config.getObjectFactory().getDefaultKey());
            ThreadLocal<WeakReference<Object>> threadLocal = (ThreadLocal<WeakReference<Object>>) TestUtil.getFieldValue(objectBucket, "threadLocal");
            Assertions.assertNull(threadLocal);//no cached pooled object in ThreadLocal
        }
    }
}
