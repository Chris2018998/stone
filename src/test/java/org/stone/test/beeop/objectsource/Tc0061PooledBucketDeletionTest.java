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
import org.stone.beeop.*;
import org.stone.beeop.exception.BeePooledObjectKeyException;
import org.stone.test.beeop.config.OsConfigFactory;
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.factory.TextBookFactory;

/**
 * @author Chris Liao
 */
public class Tc0061PooledBucketDeletionTest {

    @Test
    public void testDeleteDefaultKey() {
        //default key forbidden to deleted
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        BeeObjectFactory<String, Book> factory = config.getObjectFactory();

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            try {
                os.deleteBucket(factory.getDefaultKey());
                Assertions.fail("[Tc0049DefaultKeyTest.testDeleteDefaultKey]failed");
            } catch (Exception e) {
                Assertions.assertInstanceOf(BeePooledObjectKeyException.class, e);
                Assertions.assertEquals("Default bucket is forbidden to delete", e.getMessage());
            }

            try {
                os.deleteBucket(factory.getDefaultKey(), true);
                Assertions.fail("[Tc0049DefaultKeyTest.testDeleteDefaultKey]failed");
            } catch (Exception e) {
                Assertions.assertInstanceOf(BeePooledObjectKeyException.class, e);
                Assertions.assertEquals("Default bucket is forbidden to delete", e.getMessage());
            }
        }
    }

    @Test
    public void testDeleteKey() throws Exception {
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        TextBookFactory objectFactory = new TextBookFactory();
        config.setObjectFactory(objectFactory);
        config.setMaxActive(1);
        config.setInitialSize(1);
        config.setParkTimeForRetry(1L);//import: time control

        String defaultKey = objectFactory.getDefaultKey();
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertEquals(1, os.bucketSize());
            Assertions.assertTrue(os.existsBucket(defaultKey));

            //1:delete null key
            try {
                os.deleteBucket(null);//null key
                Assertions.fail("Failed to run testcase:[testDeletePooledKey)");
            } catch (Exception e) {
                Assertions.assertInstanceOf(BeePooledObjectKeyException.class, e);
                Assertions.assertEquals("Key can't be null", e.getMessage());
            }

            //2: Key not in
            try {
                os.deleteBucket(defaultKey);
                Assertions.fail("Failed to run testcase:[testDeletePooledKey)");
            } catch (Exception e) {
                Assertions.assertInstanceOf(BeePooledObjectKeyException.class, e);
                Assertions.assertEquals("Default bucket is forbidden to delete", e.getMessage());
            }

            //delete key
            String key2 = "Thanking in Rust";
            Assertions.assertFalse(os.existsBucket(key2));
            Assertions.assertFalse(os.deleteBucket(key2));

            //2: add a new pooled key
            BeeObjectHandle<String, Book> rustBookHandle = os.getObjectHandle(key2);
            rustBookHandle.close();
            Assertions.assertEquals(2, os.bucketSize());
            Assertions.assertTrue(os.existsBucket(key2));
            Assertions.assertTrue(os.deleteBucket(key2));
            Assertions.assertEquals(1, os.bucketSize());
            Assertions.assertFalse(os.existsBucket(key2));

            //3: test delete key by force
            os.getObjectHandle(key2);
            Assertions.assertTrue(os.existsBucket(key2));
            BeeObjectBucketMonitorVo rustKeyMonitorVo = os.getBucketMonitorVo(key2);
            Assertions.assertTrue(rustKeyMonitorVo.isReady());
            Assertions.assertEquals(0, rustKeyMonitorVo.getIdleSize());
            Assertions.assertEquals(1, rustKeyMonitorVo.getBorrowedSize());
            Assertions.assertTrue(os.deleteBucket(key2, true));
            Assertions.assertFalse(os.existsBucket(key2));
        }
    }
}
