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
import org.stone.beecp.BeeJdbcCallLog;
import org.stone.beecp.pool.DefaultJdbcLogManager;
import org.stone.test.beecp.driver.MockConnectionProperties;
import org.stone.test.beecp.objects.MockCommonConnectionFactory;

import java.sql.*;
import java.util.Collection;

/**
 * @author Chris Liao
 */
public class Tc0044SqlExecutionLogCollectTest {

    @Test
    public void testSQLExecution() throws SQLException {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcCallLogManager(new DefaultJdbcLogManager());
        config.setConnectionFactory(new MockCommonConnectionFactory());
        try (BeeDataSource ds = new BeeDataSource(config)) {
            Collection<BeeJdbcCallLog> logList = ds.getJdbcCallLog(BeeJdbcCallLog.Type_Execution_SQL);
            Assertions.assertTrue(logList.isEmpty());

            //1: test statement.execute
            try (Connection con = ds.getConnection()) {
                Statement st = con.createStatement();
                st.execute("select 1");
                st.executeQuery("select 1");
                st.executeUpdate("update user set id=1");
                st.executeLargeUpdate("update user set id=1");
            }
            logList = ds.getJdbcCallLog(BeeJdbcCallLog.Type_Execution_SQL);
            Assertions.assertTrue(logList != null && logList.size() == 4);
            for (BeeJdbcCallLog log : logList) {
                Assertions.assertNotNull(log.getParameters());
                Assertions.assertTrue(log.getStartTime() != 0);
                Assertions.assertTrue(log.getEndTime() != 0);
                Assertions.assertEquals(0, log.getSqlPreparedTime());
                Assertions.assertTrue(log.getEndTime() >= log.getStartTime());
                Assertions.assertNotNull(log.getSql());
            }

            //2: test PreparedStatement.executeXXX
            ds.clearJdbcCallLog();
            try (Connection con = ds.getConnection()) {
                PreparedStatement ps = con.prepareStatement("select 1");
                ps.execute();
                ps.executeUpdate();
                ps.executeQuery();
            }

            logList = ds.getJdbcCallLog(BeeJdbcCallLog.Type_Execution_SQL);
            Assertions.assertTrue(logList != null && logList.size() == 3);
            for (BeeJdbcCallLog log : logList) {
                Assertions.assertNull(log.getParameters());
                //Assertions.assertEquals("FastConnectionPool4L.getConnection()", log.getMethod());
                Assertions.assertTrue(log.getStartTime() != 0);
                Assertions.assertTrue(log.getEndTime() != 0);
                Assertions.assertTrue(log.getSqlPreparedTime() >= 0);
                Assertions.assertTrue(log.getEndTime() >= log.getStartTime());
                Assertions.assertNotNull(log.getSql());
            }

            //3: test CallableStatement.executeXXX
            ds.clearJdbcCallLog();
            try (Connection con = ds.getConnection()) {
                CallableStatement cs = con.prepareCall("{?=call hello()}");
                cs.execute();
                cs.executeUpdate();
                cs.executeQuery();
            }

            logList = ds.getJdbcCallLog(BeeJdbcCallLog.Type_Execution_SQL);
            Assertions.assertTrue(logList != null && logList.size() == 3);
            for (BeeJdbcCallLog log : logList) {
                //Assertions.assertEquals("FastConnectionPool4L.getConnection()", log.getMethod());
                Assertions.assertNull(log.getParameters());
                Assertions.assertTrue(log.getStartTime() != 0);
                Assertions.assertTrue(log.getEndTime() != 0);
                Assertions.assertTrue(log.getSqlPreparedTime() >= 0);
                Assertions.assertTrue(log.getEndTime() >= log.getStartTime());
                Assertions.assertNotNull(log.getSql());
            }
        }
    }

    //***************************************************************************************************************//
    //                                         exception test                                                        //
    //***************************************************************************************************************//
    @Test
    public void testSQLExecuteException() throws SQLException {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        config.setJdbcCallLogManager(new DefaultJdbcLogManager());

        MockConnectionProperties properties = new MockConnectionProperties();
        MockCommonConnectionFactory connectionFactory = new MockCommonConnectionFactory(properties);
        config.setConnectionFactory(connectionFactory);
        properties.setMockException1(new SQLException("Failed to execute sql"));
        properties.enableExceptionOnMethod("execute,executeQuery,executeUpdate,executeLargeUpdate");

        try (BeeDataSource ds = new BeeDataSource(config)) {
            Collection<BeeJdbcCallLog> logList = ds.getJdbcCallLog(BeeJdbcCallLog.Type_Execution_SQL);
            Assertions.assertTrue(logList.isEmpty());

            //1: test statement
            try (Connection con = ds.getConnection()) {
                Statement st = con.createStatement();
                try {
                    st.execute("select 1");
                    Assertions.fail();
                } catch (SQLException e) {
                    //do nothing
                }
                try {
                    st.executeQuery("select 1");
                    Assertions.fail();
                } catch (SQLException e) {
                    //do nothing
                }
                try {
                    st.executeUpdate("update user set id=1");
                    Assertions.fail();
                } catch (SQLException e) {
                    //do nothing
                }
                try {
                    st.executeLargeUpdate("update user set id=1");
                    Assertions.fail();
                } catch (SQLException e) {
                    //do nothing
                }

                logList = ds.getJdbcCallLog(BeeJdbcCallLog.Type_Execution_SQL);
                Assertions.assertTrue(logList != null && logList.size() == 4);
                for (BeeJdbcCallLog log : logList) {
                    Assertions.assertNotNull(log.getParameters());
                    Assertions.assertNotNull(log.getFailCause());
                    Assertions.assertTrue(log.getStartTime() != 0);
                    Assertions.assertTrue(log.getEndTime() != 0);
                    Assertions.assertTrue(log.getEndTime() >= log.getStartTime());
                    Assertions.assertNotNull(log.getSql());
                }
            }//statement

            //2: test PreparedStatement
            ds.clearJdbcCallLog();
            try (Connection con = ds.getConnection()) {
                PreparedStatement ps = con.prepareStatement("select 1");
                try {
                    ps.execute();
                    Assertions.fail();
                } catch (SQLException e) {
                    //do nothing
                }
                try {
                    ps.executeQuery();
                    Assertions.fail();
                } catch (SQLException e) {
                    //do nothing
                }
                try {
                    ps.executeUpdate();
                    Assertions.fail();
                } catch (SQLException e) {
                    //do nothing
                }
                logList = ds.getJdbcCallLog(BeeJdbcCallLog.Type_Execution_SQL);
                Assertions.assertTrue(logList != null && logList.size() == 3);
                for (BeeJdbcCallLog log : logList) {
                    Assertions.assertNull(log.getParameters());
                    Assertions.assertNotNull(log.getFailCause());
                    Assertions.assertTrue(log.getStartTime() != 0);
                    Assertions.assertTrue(log.getEndTime() != 0);
                    Assertions.assertTrue(log.getEndTime() >= log.getStartTime());
                    Assertions.assertNotNull(log.getSql());
                }
            }

            //3: test CallableStatement
            ds.clearJdbcCallLog();
            try (Connection con = ds.getConnection()) {
                CallableStatement cs = con.prepareCall("{?=call hello()}");
                try {
                    cs.execute();
                    Assertions.fail();
                } catch (SQLException e) {
                    //do nothing
                }
                try {
                    cs.executeQuery();
                    Assertions.fail();
                } catch (SQLException e) {
                    //do nothing
                }
                try {
                    cs.executeUpdate();
                    Assertions.fail();
                } catch (SQLException e) {
                    //do nothing
                }
                logList = ds.getJdbcCallLog(BeeJdbcCallLog.Type_Execution_SQL);
                Assertions.assertTrue(logList != null && logList.size() == 3);
                for (BeeJdbcCallLog log : logList) {
                    Assertions.assertNull(log.getParameters());
                    Assertions.assertNotNull(log.getFailCause());
                    Assertions.assertTrue(log.getStartTime() != 0);
                    Assertions.assertTrue(log.getEndTime() != 0);
                    Assertions.assertTrue(log.getEndTime() >= log.getStartTime());
                    Assertions.assertNotNull(log.getSql());
                }
            }
        }
    }
}
