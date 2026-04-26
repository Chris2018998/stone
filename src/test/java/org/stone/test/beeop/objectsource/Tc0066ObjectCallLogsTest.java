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
import org.stone.beeop.BeeMethodLog;
import org.stone.beeop.BeeObjectHandle;
import org.stone.beeop.BeeObjectSource;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.factory.TextBookFactory;

import java.util.List;

/**
 * @author Chris Liao
 */
public class Tc0066ObjectCallLogsTest {

    @Test
    public void testCallLog() throws Throwable {
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        config.setEnableLogCache(true);
        config.addObjectMethodName("getAuthor");
        config.addObjectMethodName("setAuthor");
        config.setLogTimeout(50L);
        config.setSlowCallThreshold(50L);//50 million seconds
        config.setIntervalOfClearTimeoutLogs(10L);
        TextBookFactory bookFactory = new TextBookFactory();
        config.setObjectFactory(bookFactory);

        //1: not slow
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            //call method 'setAuthor'
            try (BeeObjectHandle<String, Book> bookHandle = os.getObjectHandle()) {
                Assertions.assertTrue(os.getKeyObjectLogs(bookFactory.getDefaultKey()).isEmpty());
                bookHandle.call("setAuthor", new Class[]{String.class}, new Object[]{"Bruce Eckel2"});
                List<BeeMethodLog<String>> logList = os.getKeyObjectLogs(bookFactory.getDefaultKey());
                Assertions.assertEquals(1, logList.size());
                BeeMethodLog<String> log = logList.get(0);

                //log check
                Assertions.assertEquals(bookFactory.getDefaultKey(), log.getKey());
                Assertions.assertEquals("setAuthor", log.getMethod());
                Assertions.assertTrue(log.isSuccessful());
                Assertions.assertFalse(log.isSlow());
                Assertions.assertEquals("Bruce Eckel2", bookHandle.call("getAuthor"));

                //clear object logs
                os.clearKeyObjectLogs(bookFactory.getDefaultKey());
                Assertions.assertTrue(os.getKeyObjectLogs(bookFactory.getDefaultKey()).isEmpty());
            }
        }

        //2: slow log test
        bookFactory.setCallSleepTimeMs(100L);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            //call method 'setAuthor'
            try (BeeObjectHandle<String, Book> bookHandle = os.getObjectHandle()) {
                Assertions.assertTrue(os.getKeyObjectLogs(bookFactory.getDefaultKey()).isEmpty());
                bookHandle.call("getAuthor");
                List<BeeMethodLog<String>> logList = os.getKeyObjectLogs(bookFactory.getDefaultKey());
                Assertions.assertEquals(1, logList.size());
                BeeMethodLog<String> log = logList.get(0);

                //log check
                Assertions.assertEquals(bookFactory.getDefaultKey(), log.getKey());
                Assertions.assertEquals("getAuthor", log.getMethod());
                Assertions.assertEquals("Bruce Eckel", log.getResult());
                Assertions.assertTrue(log.isSuccessful());
                Assertions.assertTrue(log.isSlow());
                Assertions.assertTrue(log.getEndTime() > log.getStartTime());
            }
        }

        //3: Exception log test
        Exception callException = new Exception("Unknown error");
        bookFactory.setCallException(callException);
        config.setPrintRuntimeLogs(true);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            try (BeeObjectHandle<String, Book> bookHandle = os.getObjectHandle()) {
                Assertions.assertTrue(os.getKeyObjectLogs(bookFactory.getDefaultKey()).isEmpty());
                try {
                    bookHandle.call("getAuthor");
                } catch (Exception e) {
                    List<BeeMethodLog<String>> logList = os.getKeyObjectLogs(bookFactory.getDefaultKey());
                    Assertions.assertEquals(1, logList.size());
                    BeeMethodLog<String> log = logList.get(0);

                    //log check
                    Assertions.assertEquals(bookFactory.getDefaultKey(), log.getKey());
                    Assertions.assertEquals("getAuthor", log.getMethod());
                    Assertions.assertFalse(log.isSuccessful());
                    Assertions.assertTrue(log.isException());
                    Assertions.assertEquals(callException, log.getFailureCause());

                    //timer to clear timeout logs
                    Thread.sleep(100L);
                    Assertions.assertTrue(os.getKeyObjectLogs(bookFactory.getDefaultKey()).isEmpty());
                }
            }
        }
    }
}
