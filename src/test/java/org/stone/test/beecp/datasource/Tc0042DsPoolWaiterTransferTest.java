/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.datasource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.pool.FastConnectionPool;
import org.stone.beecp.pool.ProxyBaseWrapper;
import org.stone.test.beecp.objects.factory.MockConnectionFactory;
import org.stone.test.beecp.objects.threads.BorrowThread;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.stone.test.base.TestUtil.getFieldValue;
import static org.stone.test.base.TestUtil.waitUtilWaiting;
import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

public class Tc0042DsPoolWaiterTransferTest {
    private final Class PooledConnectionClass = Class.forName("org.stone.beecp.pool.PooledConnection");

    public Tc0042DsPoolWaiterTransferTest() throws ClassNotFoundException {
    }

    @Test
    public void testTransferConnection() throws Exception {
        //1: enable thread local
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(1);
        config.setSemaphoreSize(2);
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(3L));
        config.setConnectionFactory(new MockConnectionFactory());

        Method recycleMethod = FastConnectionPool.class.getDeclaredMethod("recycle", PooledConnectionClass);
        recycleMethod.setAccessible(true);
        Field pField = ProxyBaseWrapper.class.getDeclaredField("p");
        pField.setAccessible(true);

        try (BeeDataSource ds = new BeeDataSource(config)) {
            try (Connection con = ds.getConnection()) {
                BorrowThread secondBorrowThread = new BorrowThread(ds);
                secondBorrowThread.start();
                if (waitUtilWaiting(secondBorrowThread)) {
                    Object p = pField.get(con);
                    Object dsPool = getFieldValue(ds, "pool");
                    recycleMethod.invoke(dsPool, p);//<-- a using connection

                    secondBorrowThread.join();
                    Assertions.assertNotNull(secondBorrowThread.getConnection());
                }
            }
        }

        //2: disable thread local
        BeeDataSourceConfig config2 = createDefault();
        config2.setMaxActive(1);
        config2.setSemaphoreSize(2);
        config2.setParkTimeForRetry(0L);
        config.setUseThreadLocal(false);
        config2.setForceRecycleBorrowedOnClose(true);
        config2.setMaxWait(TimeUnit.SECONDS.toMillis(3L));
        config2.setConnectionFactory(new MockConnectionFactory());

        try (BeeDataSource ds = new BeeDataSource(config2)) {
            try (Connection con = ds.getConnection()) {
                BorrowThread secondBorrowThread = new BorrowThread(ds);
                secondBorrowThread.start();
                if (waitUtilWaiting(secondBorrowThread)) {
                    Object p = pField.get(con);

                    Object dsPool = getFieldValue(ds, "pool");
                    recycleMethod.invoke(dsPool, p);//<-- a using connection

                    secondBorrowThread.join();
                    Assertions.assertNotNull(secondBorrowThread.getConnection());
                }
            }
        }
    }

    @Test
    public void testTransferException() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setMaxActive(1);
        config.setSemaphoreSize(2);
        config.setParkTimeForRetry(0L);
        config.setForceRecycleBorrowedOnClose(true);
        config.setMaxWait(TimeUnit.SECONDS.toMillis(10L));
        config.setConnectionFactory(new MockConnectionFactory());

        try (BeeDataSource ds = new BeeDataSource(config)) {
            try (Connection con = ds.getConnection()) {
                Method transferExceptionMethod = FastConnectionPool.class.getDeclaredMethod("transferException", Throwable.class);
                transferExceptionMethod.setAccessible(true);

                BorrowThread secondBorrower = new BorrowThread(ds);
                secondBorrower.start();

                Object dsPool = getFieldValue(ds, "pool");
                if (waitUtilWaiting(secondBorrower)) {//block 1 second in pool instance creation
                    transferExceptionMethod.invoke(dsPool, new SQLException("Net Error"));
                    secondBorrower.join();
                    SQLException e = secondBorrower.getFailureCause();
                    Assertions.assertTrue(e != null && e.getMessage().contains("Net Error"));
                }

                BorrowThread thirdBorrower = new BorrowThread(ds);
                thirdBorrower.start();
                if (waitUtilWaiting(thirdBorrower)) {//block 1 second in pool instance creation
                    transferExceptionMethod.invoke(dsPool, new Exception("Connection created fail"));
                    thirdBorrower.join();
                    SQLException e = thirdBorrower.getFailureCause();
                    Assertions.assertTrue(e != null && e.getMessage().contains("Connection created fail"));
                }
            }
        }
    }

//    @Test
//    public void testAliveTestSuccess() throws Exception {
//        BeeDataSourceConfig config = createDefault();
//        config.setMaxActive(1);
//        config.setFairMode(true);
//        config.setSemaphoreSize(2);
//        config.setParkTimeForRetry(0L);
//        config.setAliveAssumeTime(0L);
//        config.setForceRecycleBorrowedOnClose(true);
//        config.setMaxWait(TimeUnit.SECONDS.toMillis(1L));
//
//        MockConnectionProperties properties = new MockConnectionProperties();
//        MockConnectionFactory factory = new MockConnectionFactory(properties);
//        config.setConnectionFactory(factory);
//        try (BeeDataSource ds = new BeeDataSource(config)) {
//
//            //1: get method by reflection
//            Method recycleMethod = FastConnectionPool.class.getDeclaredMethod("recycle", PooledConnectionClass);
//            recycleMethod.setAccessible(true);
//            Field pField = ProxyBaseWrapper.class.getDeclaredField("p");
//            pField.setAccessible(true);
//
//            try (Connection con = ds.getConnection()) {
//                BorrowThread secondBorrowThread = new BorrowThread(ds);
//                secondBorrowThread.start();
//                if (waitUtilWaiting(secondBorrowThread)) {
//                    Object p = pField.get(con);
//                    //p.lastAccessTime = 0L;
//                    TestUtil.setFieldValue(p, "lastAccessTime", 0);
//                    properties.setValid(true);
//
//                    Object dsPool = getFieldValue(ds, "pool");
//                    recycleMethod.invoke(dsPool, p);
//
//                    secondBorrowThread.join();
//                    Assertions.assertNotNull(secondBorrowThread.getConnection());
//                }
//            }
//        }
//    }
//
//    @Test
//    public void testCatchFailAfterTransfer() throws Exception {
//        BeeDataSourceConfig config = createDefault();
//        config.setMaxActive(1);
//        config.setFairMode(true);
//        config.setSemaphoreSize(2);
//        config.setParkTimeForRetry(0L);
//        config.setForceRecycleBorrowedOnClose(true);
//        config.setMaxWait(TimeUnit.SECONDS.toMillis(1L));
//        config.setConnectionFactory(new MockConnectionFactory());
//        try (BeeDataSource ds = new BeeDataSource(config)) {
//            Method recycleMethod = FastConnectionPool.class.getDeclaredMethod("recycle", PooledConnectionClass);
//            recycleMethod.setAccessible(true);
//            Field pField = ProxyBaseWrapper.class.getDeclaredField("p");
//            pField.setAccessible(true);
//
//            try (Connection con = ds.getConnection()) {
//                BorrowThread secondBorrowThread = new BorrowThread(ds);
//                secondBorrowThread.start();
//                if (waitUtilWaiting(secondBorrowThread)) {
//                    Object p = pField.get(con);
//                    //p.state = CON_IDLE;
//                    TestUtil.setFieldValue(p, "state", CON_IDLE);
//
//                    Object dsPool = getFieldValue(ds, "pool");
//                    recycleMethod.invoke(dsPool, p);
//
//                    secondBorrowThread.join();
//                    Assertions.assertNull(secondBorrowThread.getConnection());
//                    Assertions.assertInstanceOf(ConnectionGetTimeoutException.class, secondBorrowThread.getFailureCause());
//                }
//            }
//        }
//    }
//
//    @Test
//    public void testAliveTestFail() throws Exception {
//        BeeDataSourceConfig config = createDefault();
//        config.setMaxActive(1);
//        config.setFairMode(true);
//        config.setSemaphoreSize(2);
//        config.setParkTimeForRetry(0L);
//        config.setAliveAssumeTime(0L);
//        config.setForceRecycleBorrowedOnClose(true);
//        config.setMaxWait(TimeUnit.SECONDS.toMillis(1L));
//
//        MockConnectionProperties properties = new MockConnectionProperties();
//        MockConnectionFactory factory = new MockConnectionFactory(properties);
//        config.setConnectionFactory(factory);
//
//        try (BeeDataSource ds = new BeeDataSource(config)) {
//            //1: get method by reflection
//            Method recycleMethod = FastConnectionPool.class.getDeclaredMethod("recycle", PooledConnectionClass);
//            recycleMethod.setAccessible(true);
//            Field pField = ProxyBaseWrapper.class.getDeclaredField("p");
//            pField.setAccessible(true);
//
//            try (Connection con = ds.getConnection()) {
//                BorrowThread secondBorrowThread = new BorrowThread(ds);
//                secondBorrowThread.start();
//                if (waitUtilWaiting(secondBorrowThread)) {
//                    Object p = pField.get(con);
//
//                    //p.lastAccessTime = 0L;
//                    TestUtil.setFieldValue(p, "lastAccessTime", 0L);
//                    properties.setValid(false);
//                    Object dsPool = getFieldValue(ds, "pool");
//                    recycleMethod.invoke(dsPool, p);
//
//                    secondBorrowThread.join();
//                    Assertions.assertNull(secondBorrowThread.getConnection());
//                    Assertions.assertInstanceOf(ConnectionGetTimeoutException.class, secondBorrowThread.getFailureCause());
//                }
//            }
//        }
//    }
}
