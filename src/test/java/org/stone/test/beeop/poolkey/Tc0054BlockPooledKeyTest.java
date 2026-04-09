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

            //interruptWaitingThreads(key)
            bookFactory.setBlock(BlockWayTypes.Type_Sleep, Long.MAX_VALUE);
            String newKey = "Thinking in C++";
            ObjectBorrowThread firstThread = new ObjectBorrowThread(os, newKey);
            firstThread.start();
            if (waitUtilWaiting(firstThread)) {
                Assertions.assertEquals(1, os.getKeyMonitorVo(newKey).getCreatingSize());
                Assertions.assertEquals(0, os.getKeyMonitorVo(newKey).getCreatingTimeoutSize());
                Thread.sleep(200L);
                Assertions.assertEquals(1, os.getKeyMonitorVo(newKey).getCreatingSize());
                Assertions.assertEquals(1, os.getKeyMonitorVo(newKey).getCreatingTimeoutSize());
                List<Thread> threadList= os.interruptWaitingThreads(newKey);
                Assertions.assertTrue(threadList.contains(firstThread));
            }

            //interruptWaitingThreads()
            firstThread = new ObjectBorrowThread(os, newKey);
            String newKey2 = "Thinking in Rust";
            ObjectBorrowThread secondThread = new ObjectBorrowThread(os, newKey2);
            firstThread.start();
            secondThread.start();
            if (waitUtilWaiting(firstThread)) {
                Thread.sleep(200L);
                List<Thread> threadList= os.interruptWaitingThreads();
                Assertions.assertTrue(threadList.contains(firstThread));
                Assertions.assertTrue(threadList.contains(secondThread));
            }
        }
    }
}
