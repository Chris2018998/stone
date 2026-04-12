/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beeop.poolkey;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beeop.BeeObjectHandle;
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.test.beeop.config.OsConfigFactory;
import org.stone.test.beeop.objects.book.Book;

/**
 * @author Chris Liao
 */
public class Tc0055ObjectTimeoutTest {

    @Test
    public void testIdleTimeout() throws Exception {
        BeeObjectSourceConfig<String, Book> config = OsConfigFactory.createDefault();
        config.setInitialSize(2);
        config.setMaxActive(2);
        config.setIdleTimeout(1L);
        config.setHoldTimeout(500L);
        config.setIntervalOfClearTimeout(500L);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertEquals(2, os.getKeyMonitorVo(config.getObjectFactory().getDefaultKey()).getIdleSize());
            Thread.sleep(1000L);
            Assertions.assertEquals(0, os.getKeyMonitorVo(config.getObjectFactory().getDefaultKey()).getIdleSize());

            try (BeeObjectHandle<String, Book> handle = os.getObjectHandle()) {
                Assertions.assertEquals(1, os.getKeyMonitorVo(config.getObjectFactory().getDefaultKey()).getBorrowedSize());
                Thread.sleep(1000L);
                Assertions.assertEquals(0, os.getKeyMonitorVo(config.getObjectFactory().getDefaultKey()).getBorrowedSize());
            }
        }
    }
}
