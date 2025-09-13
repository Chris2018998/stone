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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stone.beecp.BeeDataSource;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.test.base.LogCollector;
import org.stone.test.beecp.objects.MockCommonConnectionFactory;
import org.stone.test.beecp.objects.MockConnectionTracker;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

import static org.stone.test.beecp.config.DsConfigFactory.createEmpty;

/**
 * @author Chris Liao
 */
public class Tc0041ConnectionTrackerTest {
    private Logger log = LoggerFactory.getLogger(Tc0041ConnectionTrackerTest.class);

    public static void main(String[]args)throws Exception{
        Tc0041ConnectionTrackerTest test = new Tc0041ConnectionTrackerTest();
        test.testCallableStatement();
    }

    @Test
    public void testStatement() throws Exception {
        BeeDataSourceConfig config = createEmpty();
        config.setConnectionFactory(new MockCommonConnectionFactory());
        config.setConnectionTracker(new MockConnectionTracker());
        BeeDataSource ds = new BeeDataSource(config);

        LogCollector logCollector = LogCollector.startLogCollector();
        try (Connection con = ds.getConnection()) {
            try {
                Statement st = con.createStatement();
                System.out.println("statement class:" + st.getClass().getName());
                st.execute("select * from test");
            }catch (Exception e){
                e.printStackTrace();
            }
        }catch (Exception ee){
            ee.printStackTrace();
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
        config.setConnectionTracker(new MockConnectionTracker());
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
        config.setConnectionTracker(new MockConnectionTracker());
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
}
