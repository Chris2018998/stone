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
import org.stone.test.base.LogCollector;
import org.stone.test.beecp.driver.MockConnectionProperties;
import org.stone.test.beecp.objects.MockCommonConnectionFactory;
import org.stone.test.beecp.objects.MockConnectionInterceptor;

import java.sql.*;

import static org.stone.test.beecp.config.DsConfigFactory.createEmpty;

/**
 * @author Chris Liao
 */
public class Tc0041ConnectionInterceptorTest {
    @Test
    public void testStatement() throws Exception {
        BeeDataSourceConfig config = createEmpty();
        config.setConnectionFactory(new MockCommonConnectionFactory());
        config.setConnectionInterceptor(new MockConnectionInterceptor());
        BeeDataSource ds = new BeeDataSource(config);

        LogCollector logCollector = LogCollector.startLogCollector();
        try (Connection con = ds.getConnection()) {
            Statement st = con.createStatement();
            st.execute("select * from test");
        }

        String logs = logCollector.endLogCollector();
        Assertions.assertTrue(logs.contains("beforeGetConnection"));
        Assertions.assertTrue(logs.contains("afterGetConnection"));

        Assertions.assertTrue(logs.contains("beforeExecuteSQL"));
        Assertions.assertTrue(logs.contains("afterExecuteSQL"));
    }


    @Test
    public void testPrepareStatement() throws Exception {
        BeeDataSourceConfig config = createEmpty();
        config.setConnectionFactory(new MockCommonConnectionFactory());
        config.setConnectionInterceptor(new MockConnectionInterceptor());
        BeeDataSource ds = new BeeDataSource(config);

        LogCollector logCollector = LogCollector.startLogCollector();
        try (Connection con = ds.getConnection()) {
            PreparedStatement ps = con.prepareStatement("select * from test");
            ps.executeQuery();
        }

        String logs = logCollector.endLogCollector();
        Assertions.assertTrue(logs.contains("beforeGetConnection"));
        Assertions.assertTrue(logs.contains("afterGetConnection"));

        Assertions.assertTrue(logs.contains("beforePrepareSQL"));
        Assertions.assertTrue(logs.contains("afterPrepareSQL"));

        Assertions.assertTrue(logs.contains("beforeExecutePreparedSQL"));
        Assertions.assertTrue(logs.contains("afterExecutePreparedSQL"));
    }

    @Test
    public void testCallableStatement() throws Exception {
        BeeDataSourceConfig config = createEmpty();
        config.setConnectionFactory(new MockCommonConnectionFactory());
        config.setConnectionInterceptor(new MockConnectionInterceptor());
        BeeDataSource ds = new BeeDataSource(config);

        LogCollector logCollector = LogCollector.startLogCollector();
        try (Connection con = ds.getConnection()) {
            CallableStatement ps = con.prepareCall("{hello()}");
            ps.executeQuery();
        }

        String logs = logCollector.endLogCollector();
        Assertions.assertTrue(logs.contains("beforeGetConnection"));
        Assertions.assertTrue(logs.contains("afterGetConnection"));

        Assertions.assertTrue(logs.contains("beforePrepareSQL"));
        Assertions.assertTrue(logs.contains("afterPrepareSQL"));

        Assertions.assertTrue(logs.contains("beforeExecutePreparedSQL"));
        Assertions.assertTrue(logs.contains("afterExecutePreparedSQL"));
    }

    @Test
    public void testFailOnGetConnection() throws Exception {
        BeeDataSourceConfig config = createEmpty();
        MockConnectionInterceptor tracker = new MockConnectionInterceptor();
        config.setConnectionInterceptor(tracker);
        MockCommonConnectionFactory commonConnectionFactory = new MockCommonConnectionFactory();
        SQLException failException = new SQLException("Cannot connect to the target db");
        commonConnectionFactory.setCreateException1(failException);
        config.setConnectionFactory(commonConnectionFactory);
        BeeDataSource ds = new BeeDataSource(config);

        try {
            ds.getConnection();
            Assertions.fail();
        } catch (SQLException e) {
            //do nothing
        }
        Assertions.assertSame(tracker.getFailCause(), failException);
        tracker.setFailCause(null);//clear for next test

        try {
            ds.getConnection("root", "test");
            Assertions.fail();
        } catch (SQLException e) {
            //do nothing
        }
        Assertions.assertSame(tracker.getFailCause(), failException);
    }


    @Test
    public void testFailOnGetXAConnection() throws Exception {
        BeeDataSourceConfig config = createEmpty();
        MockConnectionInterceptor tracker = new MockConnectionInterceptor();
        config.setConnectionInterceptor(tracker);
        MockCommonConnectionFactory commonConnectionFactory = new MockCommonConnectionFactory();
        SQLException failException = new SQLException("Cannot connect to the target db");
        commonConnectionFactory.setCreateException1(failException);
        config.setConnectionFactory(commonConnectionFactory);
        BeeDataSource ds = new BeeDataSource(config);

        try {
            ds.getXAConnection();
            Assertions.fail();
        } catch (SQLException e) {
            //do nothing
        }
        Assertions.assertSame(tracker.getFailCause(), failException);
        tracker.setFailCause(null);//clear for next test

        try {
            ds.getXAConnection("root", "test");
            Assertions.fail();
        } catch (SQLException e) {
            //do nothing
        }
        Assertions.assertSame(tracker.getFailCause(), failException);
    }

    @Test
    public void testFailOnPrepareSQL() throws Exception {
        BeeDataSourceConfig config = createEmpty();
        MockConnectionInterceptor tracker = new MockConnectionInterceptor();
        config.setConnectionInterceptor(tracker);

        MockConnectionProperties properties = new MockConnectionProperties();
        MockCommonConnectionFactory commonConnectionFactory = new MockCommonConnectionFactory(properties);
        SQLException failException = new SQLException("Failed to prepare sql");
        properties.enableExceptionOnMethod("prepareStatement");//exception method name
        properties.enableExceptionOnMethod("prepareCall");//exception method name
        properties.setMockException1(failException);
        config.setConnectionFactory(commonConnectionFactory);
        BeeDataSource ds = new BeeDataSource(config);

        try (Connection con = ds.getConnection()) {
            try {
                con.prepareStatement("select * from test_user");
                Assertions.fail();
            } catch (SQLException e) {
                //do nothing
            }
            Assertions.assertSame(tracker.getFailCause(), failException);
            tracker.setFailCause(null);//clear for next test

            try {
                con.prepareCall("{hello()}");
                Assertions.fail();
            } catch (SQLException e) {
                //do nothing
            }
            Assertions.assertSame(tracker.getFailCause(), failException);
            tracker.setFailCause(null);//clear for next test
        }
    }

    @Test
    public void testFailOnExecutePreparedSQL() throws Exception {
        BeeDataSourceConfig config = createEmpty();
        MockConnectionInterceptor tracker = new MockConnectionInterceptor();
        config.setConnectionInterceptor(tracker);

        MockConnectionProperties properties = new MockConnectionProperties();
        MockCommonConnectionFactory commonConnectionFactory = new MockCommonConnectionFactory(properties);
        SQLException failException = new SQLException("Failed to prepare sql");
        properties.enableExceptionOnMethod("execute");//exception method name
        properties.enableExceptionOnMethod("executeQuery");//exception method name
        properties.enableExceptionOnMethod("executeUpdate");//exception method name

        properties.setMockException1(failException);
        config.setConnectionFactory(commonConnectionFactory);
        BeeDataSource ds = new BeeDataSource(config);

        try (Connection con = ds.getConnection()) {
            //1: test on PreparedStatement
            PreparedStatement ps = con.prepareStatement("select * from test_user");
            try {
                ps.execute();
                Assertions.fail();
            } catch (SQLException e) {
                //do nothing
            }
            Assertions.assertSame(tracker.getFailCause(), failException);
            tracker.setFailCause(null);//clear for next test
            try {
                ps.executeQuery();
                Assertions.fail();
            } catch (SQLException e) {
                //do nothing
            }
            Assertions.assertSame(tracker.getFailCause(), failException);
            tracker.setFailCause(null);//clear for next test

            try {
                ps.executeUpdate();
                Assertions.fail();
            } catch (SQLException e) {
                //do nothing
            }
            Assertions.assertSame(tracker.getFailCause(), failException);
            tracker.setFailCause(null);//clear for next test

            //2: test on CallableStatement
            CallableStatement cs = con.prepareCall("{hello()}");
            try {
                cs.execute();
                Assertions.fail();
            } catch (SQLException e) {
                //do nothing
            }
            Assertions.assertSame(tracker.getFailCause(), failException);
            tracker.setFailCause(null);//clear for next test
            try {
                cs.executeQuery();
                Assertions.fail();
            } catch (SQLException e) {
                //do nothing
            }
            Assertions.assertSame(tracker.getFailCause(), failException);
            tracker.setFailCause(null);//clear for next test

            try {
                cs.executeUpdate();
                Assertions.fail();
            } catch (SQLException e) {
                //do nothing
            }
            Assertions.assertSame(tracker.getFailCause(), failException);
            tracker.setFailCause(null);//clear for next test
        }
    }

    @Test
    public void testFailOnExecuteSQL() throws Exception {
        BeeDataSourceConfig config = createEmpty();
        MockConnectionInterceptor tracker = new MockConnectionInterceptor();
        config.setConnectionInterceptor(tracker);

        MockConnectionProperties properties = new MockConnectionProperties();
        MockCommonConnectionFactory commonConnectionFactory = new MockCommonConnectionFactory(properties);
        SQLException failException = new SQLException("Failed to prepare sql");
        properties.enableExceptionOnMethod("execute");//exception method name
        properties.enableExceptionOnMethod("executeQuery");//exception method name
        properties.enableExceptionOnMethod("executeUpdate");//exception method name
        properties.enableExceptionOnMethod("executeLargeUpdate");//exception method name

        properties.setMockException1(failException);
        config.setConnectionFactory(commonConnectionFactory);
        BeeDataSource ds = new BeeDataSource(config);

        try (Connection con = ds.getConnection()) {
            //1: test on PreparedStatement
            Statement st = con.createStatement();
            try {
                st.execute("select 1 from test_user");
                Assertions.fail();
            } catch (SQLException e) {
                //do nothing
            }
            Assertions.assertSame("select 1 from test_user", tracker.getSql());
            Assertions.assertSame(tracker.getFailCause(), failException);
            tracker.setFailCause(null);//clear for next test
            try {
                st.executeUpdate("update test_user set name='Chris'");
                Assertions.fail();
            } catch (SQLException e) {
                //do nothing
            }
            Assertions.assertSame(tracker.getFailCause(), failException);
            tracker.setFailCause(null);//clear for next test

            try {
                st.executeLargeUpdate("update test_user set name='Chris'");
                Assertions.fail();
            } catch (SQLException e) {
                //do nothing
            }
            Assertions.assertSame(tracker.getFailCause(), failException);
            tracker.setFailCause(null);//clear for next test
            try {
                st.executeQuery("select 1 from test_user");
                Assertions.fail();
            } catch (SQLException e) {
                //do nothing
            }
            Assertions.assertSame(tracker.getFailCause(), failException);
            tracker.setFailCause(null);//clear for next test
        }
    }
}
