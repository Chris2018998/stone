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
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.test.beeop.objects.ObjectBorrowThread;
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.factory.BlockWayTypes;
import org.stone.test.beeop.objects.factory.TextBookFactory;

import java.util.List;

import static org.stone.test.base.TestUtil.waitUtilWaiting;

/**
 * @author Chris Liao
 */
public class Tc0054BlockPooledKeyTest {

    @Test
    public void testBlock() throws Exception {
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        config.setInitialSize(0);
        config.setMaxActive(1);
        config.setIntervalOfClearTimeout(Long.MAX_VALUE);
        config.setMaxWait(100L);//1 millisecond
        TextBookFactory bookFactory = new TextBookFactory();
        config.setObjectFactory(bookFactory);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            Assertions.assertTrue(os.existsKey(bookFactory.getDefaultKey()));

            //test block on new key
            bookFactory.setBlock(BlockWayTypes.Type_Sleep, Long.MAX_VALUE);
            String newKey = "Thinking in C++";
            ObjectBorrowThread borrowThread = new ObjectBorrowThread(os, newKey);
            borrowThread.start();
            if (waitUtilWaiting(borrowThread)) {
                Assertions.assertEquals(1, os.getKeyMonitorVo(newKey).getCreatingSize());
                Assertions.assertEquals(0, os.getKeyMonitorVo(newKey).getCreatingTimeoutSize());
                Thread.sleep(200L);
                Assertions.assertEquals(1, os.getKeyMonitorVo(newKey).getCreatingSize());
                Assertions.assertEquals(1, os.getKeyMonitorVo(newKey).getCreatingTimeoutSize());
                List<Thread> threadList= os.interruptWaitingThreads(newKey);
                Assertions.assertTrue(threadList.contains(borrowThread));
            }
        }
    }
}
