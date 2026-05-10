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
import org.stone.test.base.LogCollector;
import org.stone.test.beeop.objects.BaseThread;
import org.stone.test.beeop.objects.book.Book;
import org.stone.test.beeop.objects.factory.TextBookFactory;
import org.stone.test.beeop.objects.methodLog.LogListener1;
import org.stone.test.beeop.objects.methodLog.LogListener2;

import java.util.List;

import static org.stone.beeop.BeeMethodLog.Type_Object_Log;

/**
 * @author Chris Liao
 */
public class Tc0087PooledObjectCallLogsTest {

    @Test
    public void testSuccessLog() throws Throwable {
        //1: All calls to log cache
        BeeObjectSourceConfig<String, Book> config1 = new BeeObjectSourceConfig<>();
        config1.setEnableLogCache(true);
        config1.setPoolName("KeyPool1");
        TextBookFactory bookFactory = new TextBookFactory();
        config1.setObjectFactory(bookFactory);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config1)) {
            try (BeeObjectHandle<String, Book> bookHandle = os.getObjectHandle()) {
                Object callResult = bookHandle.call("getTitle");

                List<BeeMethodLog<String>> logList = os.getBucketObjectLogs(bookFactory.getDefaultKey());
                Assertions.assertEquals(1, logList.size());
                BeeMethodLog<String> log = logList.get(0);
                Assertions.assertNotNull(log.getId());
                Assertions.assertTrue(log.getStartTime() > 0L);
                Assertions.assertTrue(log.getEndTime() > 0L);
                Assertions.assertTrue(log.getEndTime() >= log.getStartTime());
                Assertions.assertNull(log.getParameters());
                Assertions.assertEquals(config1.getPoolName(), log.getPoolName());
                Assertions.assertEquals(bookFactory.getDefaultKey(), log.getKey());
                Assertions.assertEquals("getTitle", log.getMethod());
                Assertions.assertEquals(Thread.currentThread(), log.getCallThread());
                Assertions.assertTrue(log.isSuccessful());
                Assertions.assertFalse(log.isException());
                Assertions.assertEquals(callResult, log.getResult());

                bookHandle.call("getAuthor");
                Assertions.assertEquals(2, os.getBucketObjectLogs(bookFactory.getDefaultKey()).size());
                os.clearBucketObjectLogs(bookFactory.getDefaultKey());
                Assertions.assertEquals(0, os.getBucketObjectLogs(bookFactory.getDefaultKey()).size());
            }
        }

        //2: only cache method logs with configured method names
        BeeObjectSourceConfig<String, Book> config2 = new BeeObjectSourceConfig<>();
        config2.setEnableLogCache(true);
        config2.addObjectMethodName("getTitle");
        TextBookFactory bookFactory2 = new TextBookFactory();
        config2.setObjectFactory(bookFactory2);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config2)) {
            try (BeeObjectHandle<String, Book> bookHandle = os.getObjectHandle()) {
                bookHandle.call("getTitle");
                List<BeeMethodLog<String>> logList = os.getBucketObjectLogs(bookFactory.getDefaultKey());
                Assertions.assertEquals(1, logList.size());
                Assertions.assertTrue(logList.get(0).isSuccessful());

                bookHandle.call("getAuthor");//no log generated
                Assertions.assertEquals(1, os.getBucketObjectLogs(bookFactory.getDefaultKey()).size());
                bookHandle.call("getAuthor");//no log generated
                Assertions.assertEquals(1, os.getBucketObjectLogs(bookFactory.getDefaultKey()).size());
            }
        }
    }

    @Test
    public void testExceptionLog() throws Throwable {
        BeeObjectSourceConfig<String, Book> config1 = new BeeObjectSourceConfig<>();
        config1.setEnableLogCache(true);
        TextBookFactory bookFactory = new TextBookFactory();

        Exception callException = new Exception("Paper is not enough");
        bookFactory.addObjectMethodException("getTitle", callException);
        config1.setObjectFactory(bookFactory);

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config1)) {
            try (BeeObjectHandle<String, Book> bookHandle = os.getObjectHandle()) {
                try {
                    bookHandle.call("getTitle");
                } catch (Exception e) {
                    Assertions.assertEquals(e, callException);
                }

                List<BeeMethodLog<String>> logList = os.getBucketObjectLogs(bookFactory.getDefaultKey());
                Assertions.assertEquals(1, logList.size());
                Assertions.assertTrue(logList.get(0).isException());
                Assertions.assertFalse(logList.get(0).isSuccessful());
                Assertions.assertEquals(callException, logList.get(0).getFailureCause());
            }
        }
    }

    @Test
    public void testSlowLog() throws Throwable {
        BeeObjectSourceConfig<String, Book> config1 = new BeeObjectSourceConfig<>();
        config1.setEnableLogCache(true);
        config1.setSlowCallThreshold(10L);
        TextBookFactory bookFactory = new TextBookFactory();
        bookFactory.addObjectMethodPauseTime("getTitle", 500L);
        config1.setObjectFactory(bookFactory);

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config1)) {
            try (BeeObjectHandle<String, Book> bookHandle = os.getObjectHandle()) {
                bookHandle.call("getTitle");

                List<BeeMethodLog<String>> logList = os.getBucketObjectLogs(bookFactory.getDefaultKey());
                Assertions.assertEquals(1, logList.size());
                Assertions.assertTrue(logList.get(0).isSuccessful());
                Assertions.assertTrue(logList.get(0).isSlow());
            }
        }
    }

    @Test
    public void testLogTimeoutClear() throws Throwable {
        BeeObjectSourceConfig<String, Book> config1 = new BeeObjectSourceConfig<>();
        config1.setEnableLogCache(true);
        config1.setLogTimeout(1L);
        config1.setIntervalOfClearTimeoutLogs(100L);
        TextBookFactory bookFactory = new TextBookFactory();
        config1.setObjectFactory(bookFactory);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config1)) {
            try (BeeObjectHandle<String, Book> bookHandle = os.getObjectHandle()) {
                bookHandle.call("getTitle");
                List<BeeMethodLog<String>> logList = os.getBucketObjectLogs(bookFactory.getDefaultKey());
                Assertions.assertEquals(1, logList.size());
                Assertions.assertTrue(logList.get(0).isSuccessful());
                Thread.sleep(500L);
                logList = os.getBucketObjectLogs(bookFactory.getDefaultKey());
                Assertions.assertEquals(0, logList.size());
            }
        }
    }

    @Test
    public void testLongCallLog() throws Throwable {
        BeeObjectSourceConfig<String, Book> config1 = new BeeObjectSourceConfig<>();
        config1.setEnableLogCache(true);
        config1.setLogTimeout(Long.MAX_VALUE);
        config1.setInterruptSlowCall(false);
        config1.setSlowCallThreshold(10L);
        config1.setIntervalOfClearTimeoutLogs(10L);
        TextBookFactory bookFactory = new TextBookFactory();
        bookFactory.addObjectMethodPauseTime("getTitle", Long.valueOf(Long.MAX_VALUE));

        config1.setObjectFactory(bookFactory);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config1)) {
            MethodCallThread callThread1 = new MethodCallThread(os);
            callThread1.start();
            Thread.sleep(100L);
            List<BeeMethodLog<String>> logList = os.getBucketObjectLogs(bookFactory.getDefaultKey());
            BeeMethodLog<String> log = logList.get(0);
            Assertions.assertTrue(log.isLongRunning());
            log.getCallThread().interrupt();
            callThread1.join();
            Assertions.assertNotNull(callThread1.getFailureCause());
        }

        config1.setInterruptSlowCall(true);
        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config1)) {
            MethodCallThread callThread2 = new MethodCallThread(os);
            callThread2.start();
            MethodCallThread callThread3 = new MethodCallThread(os);
            callThread3.start();
            callThread2.join();
            callThread3.join();
            Assertions.assertNotNull(callThread2.getFailureCause());
            Assertions.assertNotNull(callThread3.getFailureCause());
        }
    }

    @Test
    public void testLogListenerChange() throws Throwable {
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        config.setPrintRuntimeLogs(true);
        config.setEnableLogCache(true);
        config.setInterruptSlowCall(true);
        config.setSlowCallThreshold(10L);
        config.setLogTimeout(Long.MAX_VALUE);
        config.setIntervalOfClearTimeoutLogs(10L);
        TextBookFactory bookFactory = new TextBookFactory();
        bookFactory.addObjectMethodPauseTime("getTitle", Long.valueOf(Long.MAX_VALUE));
        config.setObjectFactory(bookFactory);
        config.setLogListener(new LogListener1());

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            LogCollector logCollector1 = LogCollector.startLogCollector();
            MethodCallThread callThread1 = new MethodCallThread(os);
            callThread1.start();
            callThread1.join();
            Assertions.assertNotNull(callThread1.getFailureCause());
            String logs1 = logCollector1.endLogCollector();
            Assertions.assertTrue(logs1.contains("LogListener1.onMethodStart"));
            Assertions.assertTrue(logs1.contains("LogListener1.onMethodEnd"));
            // Assertions.assertTrue(logs1.contains("LogListener1.onLongRunningDetected"));

            os.changeBucketLogListener(bookFactory.getDefaultKey(), new LogListener2("LogListener2"));
            LogCollector logCollector2 = LogCollector.startLogCollector();
            MethodCallThread callThread2 = new MethodCallThread(os);
            callThread2.start();
            callThread2.join();
            Assertions.assertNotNull(callThread2.getFailureCause());
            String logs2 = logCollector2.endLogCollector();
            Assertions.assertTrue(logs2.contains("LogListener2.onMethodStart"));
            Assertions.assertTrue(logs2.contains("LogListener2.onMethodEnd"));
            //Assertions.assertTrue(logs2.contains("LogListener2.onLongRunningDetected"));
        }
    }

    @Test
    public void testLogListenerException() throws Throwable {
        BeeObjectSourceConfig<String, Book> config = new BeeObjectSourceConfig<>();
        config.setEnableLogCache(true);
        config.setInterruptSlowCall(true);
        config.setSlowCallThreshold(10L);
        config.setLogTimeout(Long.MAX_VALUE);
        config.setIntervalOfClearTimeoutLogs(10L);
        TextBookFactory bookFactory = new TextBookFactory();
        config.setObjectFactory(bookFactory);

        try (BeeObjectSource<String, Book> os = new BeeObjectSource<>(config)) {
            LogListener1 listener = new LogListener1();
            listener.setTargetLogType(Type_Object_Log);
            os.changeBucketLogListener(bookFactory.getDefaultKey(), listener);

            //1: exception thrown from onMethodStart
            String message1 = "Known error in onMethodStart";
            listener.addMethodException("onMethodStart", new Exception(message1));
            MethodCallThread callThread1 = new MethodCallThread(os);
            callThread1.start();
            callThread1.join();
            Assertions.assertEquals(message1, callThread1.getFailureCause().getMessage());

            //2: exception thrown from onMethodEnd
            listener.removeMethodException("onMethodStart");
            String message2 = "Known error in onMethodEnd";
            listener.addMethodException("onMethodEnd", new Exception(message2));
            MethodCallThread callThread2 = new MethodCallThread(os);
            callThread2.start();
            callThread2.join();
            Assertions.assertEquals(message2, callThread2.getFailureCause().getMessage());
        }
    }

    private static class MethodCallThread extends BaseThread {
        public MethodCallThread(BeeObjectSource<String, Book> os) {
            this.objectSource = os;
        }

        public void run() {
            try (BeeObjectHandle<String, Book> bookHandle = objectSource.getObjectHandle()) {
                bookHandle.call("getTitle");
            } catch (Throwable e) {
                this.failureCause = e;
            }
        }
    }
}
