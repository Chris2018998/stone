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
            connectionProperties.parkWhenCallMethod("execute,executeQuery,executeUpdate,executeLargeUpdate");
            connectionProperties.setParkNanos(TimeUnit.MILLISECONDS.toNanos(100L));
            MockConnectionFactory connectionFactory = new MockConnectionFactory(connectionProperties);
            ds.setConnectionFactory(connectionFactory);

            try (Connection con = ds.getConnection(); Statement st = con.createStatement()) {
                Thread sqlThread = new Thread(() -> {
                    try {
                        st.execute("select 1");
                        st.executeQuery("select 1");
                        st.executeUpdate("update user set id=1");
                        st.executeLargeUpdate("update user set id=1");
                        try (PreparedStatement ps = con.prepareStatement("select 1")) {
                            ps.execute();
                        }
                    } catch (SQLException e) {
                        //do nothing
                    }
                });
                sqlThread.start();
                sqlThread.join();

                List<BeeJdbcEventLog> logList = ds.getEventLog(Type_SQL_Execution);
                Assertions.assertEquals(5, logList.size());
                for (BeeJdbcEventLog log : logList) {
                    Assertions.assertTrue(log.isSlow());
                    Assertions.assertTrue(log.isHandled());
                }
            }//execute sql
        }
    }
}
