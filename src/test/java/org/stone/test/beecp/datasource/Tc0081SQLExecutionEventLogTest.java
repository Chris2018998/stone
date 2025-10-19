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
import org.stone.beecp.BeeJdbcEventLog;
import org.stone.beecp.pool.DefaultJdbcEventLogHandler;
import org.stone.beecp.pool.DefaultJdbcEventLogManager;
import org.stone.test.base.TestUtil;
import org.stone.test.beecp.driver.MockConnectionProperties;
import org.stone.test.beecp.objects.factory.MockConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.stone.beecp.BeeJdbcEventLog.Type_SQL_Execution;

/**
 * @author Chris Liao
 */
public class Tc0081SQLExecutionEventLogTest {

    @Test
    public void testExceptionLog() throws SQLException {
        try (BeeDataSource ds = new BeeDataSource()) {
            ds.setEventLogManager(new DefaultJdbcEventLogManager());
            ds.setEventLogHandler(new DefaultJdbcEventLogHandler());
            ds.setEventLogHandledBySyncMode(true);//sync mode

            MockConnectionProperties connectionProperties = new MockConnectionProperties();
            connectionProperties.throwsExceptionWhenCallMethod("execute,executeQuery,executeUpdate,executeLargeUpdate");
            connectionProperties.setMockException1(new SQLException("Failed to execute sql,because database was down"));
            MockConnectionFactory connectionFactory = new MockConnectionFactory(connectionProperties);
            ds.setConnectionFactory(connectionFactory);

            try (Connection con = ds.getConnection(); Statement st = con.createStatement()) {
                try {
                    st.execute("select 1");
                    Assertions.fail("[testExceptionLog]test failed");
                } catch (SQLException e) {
                    //do nothing
                }
                try {
                    st.executeQuery("select 1");
                    Assertions.fail("[testExceptionLog]test failed");
                } catch (SQLException e) {
                    //do nothing
                }
                try {
                    st.executeUpdate("update user set id=1");
                    Assertions.fail("[testExceptionLog]test failed");
                } catch (SQLException e) {
                    //do nothing
                }
                try {
                    st.executeLargeUpdate("update user set id=1");
                    Assertions.fail("[testExceptionLog]test failed");
                } catch (SQLException e) {
                    //do nothing
                }

                List<BeeJdbcEventLog> logList = ds.getEventLog(Type_SQL_Execution);
                Assertions.assertEquals(4, logList.size());
                for (BeeJdbcEventLog log : logList) {
                    Assertions.assertNotNull(log.getParameters());
                    Assertions.assertNotNull(log.getSql());
                    Assertions.assertTrue(log.isException());
                    Assertions.assertTrue(log.isHandled());
                }
            }
        }
    }

    @Test
    public void testSlowLog() throws Exception {
        try (BeeDataSource ds = new BeeDataSource()) {
            ds.setEventLogManager(new DefaultJdbcEventLogManager());
            ds.setEventLogHandler(new DefaultJdbcEventLogHandler());
            ds.setEventLogHandledBySyncMode(true);//sync mode
            ds.setSlowSQLThreshold(50L);

            MockConnectionProperties connectionProperties = new MockConnectionProperties();
            connectionProperties.parkWhenCallMethod("execute,executeQuery,executeUpdate,executeLargeUpdate,prepareStatement");
            connectionProperties.setParkNanos(TimeUnit.MILLISECONDS.toNanos(100L));
            MockConnectionFactory connectionFactory = new MockConnectionFactory(connectionProperties);
            ds.setConnectionFactory(connectionFactory);

            //1: test statement
            try (Connection con = ds.getConnection(); Statement st = con.createStatement()) {
                Thread statementThread = new Thread(() -> {
                    try {
                        st.execute("select 1");
                        st.executeQuery("select 1");
                        st.executeUpdate("update user set id=1");
                        st.executeLargeUpdate("update user set id=1");
                    } catch (SQLException e) {
                        //do nothing
                    }
                });
                statementThread.start();
                statementThread.join();

                List<BeeJdbcEventLog> logList = ds.getEventLog(Type_SQL_Execution);
                Assertions.assertEquals(4, logList.size());
                for (BeeJdbcEventLog log : logList) {
                    Assertions.assertTrue(log.isSlow());
                    Assertions.assertTrue(log.isHandled());
                }
            }//execute statement sql

            //2: clear
            ds.clearEventLog(Type_SQL_Execution);
            Assertions.assertTrue(ds.getEventLog(Type_SQL_Execution).isEmpty());

            //3: test PreparedStatement
            try (Connection con = ds.getConnection()) {
                Thread preparedStatementThread = new Thread(() -> {
                    try (PreparedStatement ps1 = con.prepareStatement("select 1")) {
                        ps1.execute();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }

                    try (PreparedStatement ps2 = con.prepareStatement("update test_user set name =? where id=?")) {
                        ps2.setString(1, "Chris");
                        ps2.setLong(2, 666888);
                        ps2.executeUpdate();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });
                preparedStatementThread.start();
                preparedStatementThread.join();

                List<BeeJdbcEventLog> logList = ds.getEventLog(Type_SQL_Execution);
                Assertions.assertEquals(1, logList.size());
                for (BeeJdbcEventLog log : logList) {
                    Assertions.assertTrue(log.getSqlPreparedTime() > 0L);
                    Assertions.assertTrue(log.isSlow());
                    Assertions.assertTrue(log.isHandled());

                    try {
                        Assertions.assertFalse(log.cancelStatement());
                    } catch (Exception e) {
                        if (e instanceof NullPointerException) {
                            Assertions.fail("[testSlowLog]Test failed");
                        }
                    }
                }
            }//execute PreparedStatement sql
        }
    }

    @Test
    public void testCancelStatement() throws Exception {
        try (BeeDataSource ds = new BeeDataSource()) {
            ds.setEventLogManager(new DefaultJdbcEventLogManager());
            ds.setEventLogHandler(new DefaultJdbcEventLogHandler());
            ds.setEventLogHandledBySyncMode(true);//sync mode
            ds.setSlowSQLThreshold(50L);

            MockConnectionProperties connectionProperties = new MockConnectionProperties();
            connectionProperties.parkWhenCallMethod("execute");//blocking execute method
            connectionProperties.setParkNanos(TimeUnit.MILLISECONDS.toNanos(Long.MAX_VALUE));
            MockConnectionFactory connectionFactory = new MockConnectionFactory(connectionProperties);
            ds.setConnectionFactory(connectionFactory);

            try (Connection con = ds.getConnection()) {
                StatementThread statementThread1 = new StatementThread(con);
                statementThread1.start();
                if (TestUtil.waitUtilWaiting(statementThread1)) {
                    List<BeeJdbcEventLog> logList = ds.getEventLog(Type_SQL_Execution);
                    Assertions.assertEquals(1, logList.size());
                    BeeJdbcEventLog log = logList.get(0);
                    Assertions.assertTrue(log.isRunning());
                    Assertions.assertTrue(log.cancelStatement());
                }

                Assertions.assertFalse(ds.cancelStatement(null));
                Assertions.assertFalse(ds.cancelStatement(new Object()));
                StatementThread statementThread2 = new StatementThread(con);
                statementThread2.start();
                if (TestUtil.waitUtilWaiting(statementThread2)) {
                    List<BeeJdbcEventLog> logList = ds.getEventLog(Type_SQL_Execution);
                    Assertions.assertEquals(2, logList.size());
                    BeeJdbcEventLog log = logList.get(0);
                    Assertions.assertTrue(log.isRunning());
                    Assertions.assertTrue(ds.cancelStatement(log.getId()));
                }
            }//connection
        }
    }

    private static class StatementThread extends Thread {
        private final Connection con;

        StatementThread(Connection con) {
            this.con = con;
        }

        public void run() {
            try (PreparedStatement ps1 = con.prepareStatement("select 1")) {
                ps1.execute();//blocking test
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
