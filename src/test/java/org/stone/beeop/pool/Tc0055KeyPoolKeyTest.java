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
import org.stone.beeop.BeeObjectPoolMonitorVo;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.objects.JavaBookFactory;
import org.stone.beeop.objects.JavaBookTypeKey;
import org.stone.beeop.pool.exception.ObjectKeyException;

import static org.stone.beeop.config.OsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0055KeyPoolKeyTest extends TestCase {

    public void testNullKey() throws Exception {
        BeeObjectSourceConfig config = createDefault();
        config.setInitialSize(2);
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        KeyedObjectPool pool = new KeyedObjectPool();
        pool.init(config);

        try {
            pool.clear(null);
        } catch (ObjectKeyException e) {
            Assert.assertTrue(e.getMessage().contains("Key can't be null or empty"));
        }

        try {
            pool.deleteKey(null);
        } catch (ObjectKeyException e) {
            Assert.assertTrue(e.getMessage().contains("Key can't be null or empty"));
        }
    }

    public void testClearWithKey() throws Exception {
        JavaBookTypeKey defaultKey = new JavaBookTypeKey();
        JavaBookFactory javaBookFactory = new JavaBookFactory();
        javaBookFactory.setDefaultKey(defaultKey);
        BeeObjectSourceConfig config = new BeeObjectSourceConfig();
        config.setObjectFactory(javaBookFactory);
        config.setInitialSize(2);
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        KeyedObjectPool pool = new KeyedObjectPool();
        pool.init(config);

        //1: default key
        Assert.assertEquals(2, pool.getMonitorVo(defaultKey).getIdleSize());
        pool.clear(defaultKey);
        Assert.assertEquals(0, pool.getMonitorVo(defaultKey).getIdleSize());
        try {
            pool.deleteKey(new JavaBookTypeKey());
        } catch (ObjectKeyException e) {
            Assert.assertTrue(e.getMessage().contains("Default key is forbidden to delete"));
        }

        //2: clear with new key
        Object simpleKey = "TestKey";
        pool.getObjectHandle(simpleKey);
        BeeObjectPoolMonitorVo categoryMonitorVo = pool.getMonitorVo(simpleKey);
        Assert.assertEquals(1, categoryMonitorVo.getIdleSize());
        Assert.assertEquals(1, categoryMonitorVo.getBorrowedSize());
        pool.clear(simpleKey, true);
        categoryMonitorVo = pool.getMonitorVo(simpleKey);
        Assert.assertEquals(0, categoryMonitorVo.getIdleSize());
        Assert.assertEquals(0, categoryMonitorVo.getBorrowedSize());

        Assert.assertTrue(pool.exists(simpleKey));
        pool.deleteKey(simpleKey);
        Assert.assertFalse(pool.exists(simpleKey));
    }
}
