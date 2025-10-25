/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.test.beecp.datasource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeMethodLog;
import org.stone.test.beecp.objects.factory.MockConnectionFactory;
import org.stone.test.beecp.objects.factory.MockXaConnectionFactory;
import org.stone.test.beecp.objects.jdbclog.DefaultMethodLogHandler;
import org.stone.test.beecp.objects.threads.BorrowThread;

import javax.sql.XAConnection;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.stone.beecp.BeeMethodLog.Type_Connection_Get;

/**
 * @author Chris Liao
 */
public class Tc0080ConnectionGetLogTest {

    @Test
    public void testExceptionLog() throws SQLException {
        //1: get connection
        try (BeeDataSource ds = new BeeDataSource()) {

            ds.setEnableMethodLogCache(true);
            ds.setMethodLogHandler(new DefaultMethodLogHandler());
            MockConnectionFactory connectionFactory = new MockConnectionFactory();
            connectionFactory.setFailCause(new SQLException("Failed to connect database"));
            ds.setConnectionFactory(connectionFactory);

            try (Connection ignored = ds.getConnection()) {
                Assertions.fail("[testConnectionExceptionLog]Test failed");
            } catch (SQLException e) {
                Assertions.assertTrue(ds.isEnabledMethodLogCache());
                List<BeeMethodLog> logList = ds.getMethodLog(Type_Connection_Get);
                Assertions.assertEquals(1, logList.size());
                BeeMethodLog log = logList.get(0);
                Assertions.assertNotNull(log.getId());
                Assertions.assertEquals(Type_Connection_Get, log.getType());

                Assertions.assertEquals("FastConnectionPool.getConnection()", log.getMethod());
                Assertions.assertFalse(log.isSuccessful());
                Assertions.assertTrue(log.isException());
                Assertions.assertFalse(log.isRunning());
                Assertions.assertNull(log.getResult());
                Assertions.assertNotNull(log.getFailCause());

                Assertions.assertTrue(log.getStartTime() != 0);
                Assertions.assertTrue(log.getEndTime() != 0);
                Assertions.assertTrue(log.getEndTime() >= log.getStartTime());
            }
        }

        //2: get XA connection
        try (BeeDataSource ds = new BeeDataSource()) {
            //ds.setEventLogManager(new MethodLogCache());
            ds.setMethodLogHandler(new DefaultMethodLogHandler());
            ds.setEnableMethodLogCache(true);//sync mode
            MockXaConnectionFactory xaConnectionFactory = new MockXaConnectionFactory();
            xaConnectionFactory.setFailCause(new SQLException("Failed to connect database"));
            ds.setXaConnectionFactory(xaConnectionFactory);

            try {
                XAConnection xaCon = ds.getXAConnection();
                try (Connection ignored = xaCon.getConnection()) {
                    Assertions.fail("[testConnectionExceptionLog]Test failed");
                }
            } catch (SQLException e) {
                Assertions.assertTrue(ds.isEnabledMethodLogCache());
                List<BeeMethodLog> logList = ds.getMethodLog(Type_Connection_Get);
                Assertions.assertEquals(1, logList.size());
                BeeMethodLog log = logList.get(0);
                Assertions.assertNotNull(log.getId());
                Assertions.assertEquals(Type_Connection_Get, log.getType());

                Assertions.assertEquals("FastConnectionPool.getXAConnection()", log.getMethod());
                Assertions.assertTrue(log.isException());
                Assertions.assertFalse(log.isSuccessful());
                Assertions.assertFalse(log.isRunning());
                Assertions.assertNull(log.getResult());
                Assertions.assertTrue(log.getStartTime() != 0);
                Assertions.assertTrue(log.getEndTime() != 0);
                Assertions.assertTrue(log.getEndTime() >= log.getStartTime());

                logList = ds.clearMethodLog(Type_Connection_Get);
                Assertions.assertEquals(1, logList.size());
            }
        }
    }

    @Test
    public void testSlowLog() throws Exception {
        //1: get connection
        try (BeeDataSource ds = new BeeDataSource()) {
            //ds.setEventLogManager(new MethodLogCache());
            ds.setMethodLogHandler(new DefaultMethodLogHandler());
            ds.setEnableMethodLogCache(true);//sync mode
            ds.setSlowConnectionThreshold(100L);
            MockConnectionFactory connectionFactory = new MockConnectionFactory();
            connectionFactory.setNeedPark(true);
            connectionFactory.setParkNanos(TimeUnit.MILLISECONDS.toNanos(500L));
            ds.setConnectionFactory(connectionFactory);

            BorrowThread borrowThread = new BorrowThread(ds);
            borrowThread.start();
            borrowThread.join();

            Assertions.assertTrue(ds.isEnabledMethodLogCache());
            List<BeeMethodLog> logList = ds.getMethodLog(Type_Connection_Get);
            Assertions.assertEquals(1, logList.size());
            BeeMethodLog log = logList.get(0);
            Assertions.assertNotNull(log.getId());
            Assertions.assertEquals(Type_Connection_Get, log.getType());
            Assertions.assertTrue(log.isSlow());
            Assertions.assertTrue(log.isSuccessful());
            Assertions.assertFalse(log.isException());
        }

        //2: get XA connection
        try (BeeDataSource ds = new BeeDataSource()) {
            //ds.setEventLogManager(new MethodLogCache());
            ds.setMethodLogHandler(new DefaultMethodLogHandler());
            ds.setEnableMethodLogCache(true);//sync mode
            ds.setSlowConnectionThreshold(100L);
            MockXaConnectionFactory xaConnectionFactory = new MockXaConnectionFactory();
            xaConnectionFactory.setNeedPark(true);
            xaConnectionFactory.setParkNanos(TimeUnit.MILLISECONDS.toNanos(500L));
            ds.setXaConnectionFactory(xaConnectionFactory);

            BorrowThread borrowThread = new BorrowThread(ds, null, true);
            borrowThread.start();
            borrowThread.join();

            List<BeeMethodLog> logList = ds.getMethodLog(Type_Connection_Get);
            BeeMethodLog log = logList.get(0);
            Assertions.assertEquals(Type_Connection_Get, log.getType());
            Assertions.assertTrue(log.isSlow());
        }
    }
}
