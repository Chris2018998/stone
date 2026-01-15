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
import org.stone.beeop.BeeObjectKeyMonitorVo;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.exception.BeePooledObjectKeyException;
import org.stone.beeop.pool.ObjectPool;
import org.stone.test.beeop.objects.JavaBookFactory;
import org.stone.test.beeop.objects.JavaBookTypeKey;

import static org.stone.test.beeop.config.OsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0055KeyPoolKeyTest {

    @Test
    public void testNullKey() throws Exception {
        BeeObjectSourceConfig config = createDefault();
        config.setInitialSize(2);
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        ObjectPool pool = new ObjectPool();
        pool.start(config);

        try {
            pool.clearKeyObjects(null);
        } catch (BeePooledObjectKeyException e) {
            Assertions.assertTrue(e.getMessage().contains("Key can't be null"));
        }

        try {
            pool.deleteKey(null);
        } catch (BeePooledObjectKeyException e) {
            Assertions.assertTrue(e.getMessage().contains("Key can't be null"));
        }
    }

    @Test
    public void testClearWithKey() throws Exception {
        JavaBookTypeKey defaultKey = new JavaBookTypeKey();
        JavaBookFactory javaBookFactory = new JavaBookFactory();
        javaBookFactory.setDefaultKey(defaultKey);
        BeeObjectSourceConfig config = new BeeObjectSourceConfig();
        config.setObjectFactory(javaBookFactory);
        config.setInitialSize(2);
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        ObjectPool pool = new ObjectPool();
        pool.start(config);

        //1: default key
        Assertions.assertEquals(2, pool.getKeyMonitorVo(defaultKey).getIdleSize());
        pool.clearKeyObjects(defaultKey);
        Assertions.assertEquals(0, pool.getKeyMonitorVo(defaultKey).getIdleSize());
        try {
            pool.deleteKey(new JavaBookTypeKey());
        } catch (BeePooledObjectKeyException e) {
            Assertions.assertTrue(e.getMessage().contains("Default key is forbidden to delete"));
        }

        //2: clear with new key
        Object simpleKey = "TestKey";
        pool.getObjectHandle(simpleKey);
        BeeObjectKeyMonitorVo categoryMonitorVo = pool.getKeyMonitorVo(simpleKey);
        Assertions.assertEquals(1, categoryMonitorVo.getIdleSize());
        Assertions.assertEquals(1, categoryMonitorVo.getBorrowedSize());
        pool.clearKeyObjects(simpleKey, true);
        categoryMonitorVo = pool.getKeyMonitorVo(simpleKey);
        Assertions.assertEquals(0, categoryMonitorVo.getIdleSize());
        Assertions.assertEquals(0, categoryMonitorVo.getBorrowedSize());

        Assertions.assertTrue(pool.existsKey(simpleKey));
        pool.deleteKey(simpleKey);
        Assertions.assertFalse(pool.existsKey(simpleKey));
    }
}
