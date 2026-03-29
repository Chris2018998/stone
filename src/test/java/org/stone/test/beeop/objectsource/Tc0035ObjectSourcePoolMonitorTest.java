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
import org.stone.test.beeop.config.OsConfigFactory;
import org.stone.test.beeop.objects.book.Book;

/**
 * @author Chris Liao
 */
public class Tc0035ObjectSourcePoolMonitorTest {

    @Test
    public void testGetMonitor() throws Exception {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        BeeObjectFactory<String, Book> factory = config.getObjectFactory();
        config.setInitialSize(2);
        config.setMaxActive(2);
        int semaphoreSize = 1;
        config.setPoolName("BeeOP1");
        config.setSemaphoreSize(semaphoreSize);
        String defaultKey = factory.getDefaultKey();

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertFalse(os.isClosed());
            BeeObjectPoolMonitorVo poolMonitorVo = os.getPoolMonitorVo(false);
            Assertions.assertFalse(poolMonitorVo.isNew());
            Assertions.assertTrue(poolMonitorVo.isReady());
            Assertions.assertNull(poolMonitorVo.getKeyMonitorVos());
            Assertions.assertNull(poolMonitorVo.getKeyMonitorVo(defaultKey));
            Assertions.assertEquals(config.getPoolName(), poolMonitorVo.getPoolName());

            poolMonitorVo = os.getPoolMonitorVo(true);
            Assertions.assertEquals(1, poolMonitorVo.getKeyMonitorVos().size());

            BeeObjectKeyMonitorVo keyMonitorVo = poolMonitorVo.getKeyMonitorVo(defaultKey);
            Assertions.assertNotNull(keyMonitorVo);
            Assertions.assertEquals(defaultKey, keyMonitorVo.getKeyName());
            Assertions.assertTrue(keyMonitorVo.isReady());
            Assertions.assertEquals(2, keyMonitorVo.getIdleSize());
            Assertions.assertEquals(0, keyMonitorVo.getBorrowedSize());
            Assertions.assertEquals(0, keyMonitorVo.getCreatingSize());
            Assertions.assertEquals(0, keyMonitorVo.getCreatingTimeoutSize());
            Assertions.assertEquals(semaphoreSize, keyMonitorVo.getSemaphoreRemainSize());
            Assertions.assertEquals(0, keyMonitorVo.getSemaphoreWaitingSize());
            Assertions.assertEquals(0, keyMonitorVo.getTransferWaitingSize());
        }
    }
}
