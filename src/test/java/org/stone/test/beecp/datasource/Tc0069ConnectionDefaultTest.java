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
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.test.base.LogCollector;
import org.stone.test.beecp.driver.MockConnectionProperties;
import org.stone.test.beecp.objects.factory.MockConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;

import static org.stone.test.base.LogCollector.startLogCollector;
import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0069ConnectionDefaultTest {

    @Test
    public void testEnableDefault() throws Exception {
        BeeDataSourceConfig config1 = createDefault();
        config1.setInitialSize(1);
        //enable all default
        config1.setUseDefaultSchema(true);
        config1.setUseDefaultCatalog(true);
        config1.setUseDefaultReadOnly(true);
        config1.setUseDefaultAutoCommit(true);
        config1.setUseDefaultTransactionIsolation(true);
        try (BeeDataSource ds = new BeeDataSource(config1)) {
            try (Connection con1 = ds.getConnection()) {
                Assertions.assertTrue(con1.getAutoCommit());
                Assertions.assertFalse(con1.isReadOnly());
                Assertions.assertEquals(0, con1.getTransactionIsolation());
                Assertions.assertNull(con1.getSchema());
                Assertions.assertNull(con1.getCatalog());
            }
        }

        BeeDataSourceConfig config2 = createDefault();
        config2.setInitialSize(1);
        //enable all default
        config2.setUseDefaultSchema(true);
        config2.setUseDefaultCatalog(true);
        config2.setUseDefaultReadOnly(true);
        config2.setUseDefaultAutoCommit(true);
        config2.setUseDefaultTransactionIsolation(true);

        //set defaults to config
        config2.setDefaultReadOnly(true);
        config2.setDefaultAutoCommit(true);
        config2.setDefaultTransactionIsolationCode(1);
        config2.setDefaultSchema("schema");
        config2.setDefaultCatalog("catalog");

        try (BeeDataSource ds = new BeeDataSource(config2)) {
            try (Connection con2 = ds.getConnection()) {
                Assertions.assertTrue(con2.getAutoCommit());
                Assertions.assertTrue(con2.isReadOnly());
                Assertions.assertEquals(1, con2.getTransactionIsolation());
                Assertions.assertEquals("schema", con2.getSchema());
                Assertions.assertEquals("catalog", con2.getCatalog());
            }
        }
    }

    @Test
    public void testDisableDefault() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        //disable all default
        config.setPrintRuntimeLogs(true);
        config.setUseDefaultSchema(false);
        config.setUseDefaultCatalog(false);
        config.setUseDefaultSchema(false);
        config.setUseDefaultReadOnly(false);
        config.setUseDefaultAutoCommit(false);
        config.setUseDefaultTransactionIsolation(false);
        try (BeeDataSource ds = new BeeDataSource(config)) {
            try (Connection con = ds.getConnection()) {
                Assertions.assertTrue(con.getAutoCommit());
                Assertions.assertFalse(con.isReadOnly());
                Assertions.assertEquals(0, con.getTransactionIsolation());
                Assertions.assertNull(con.getSchema());
                Assertions.assertNull(con.getCatalog());
            }
        }

        BeeDataSourceConfig config2 = createDefault();
        config2.setInitialSize(1);
        //disable all default
        config2.setPrintRuntimeLogs(true);
        config2.setUseDefaultSchema(false);
        config2.setUseDefaultCatalog(false);
        config2.setUseDefaultSchema(false);
        config2.setUseDefaultReadOnly(false);
        config2.setUseDefaultAutoCommit(false);
        config2.setUseDefaultTransactionIsolation(false);

        //set defaults to config
        config2.setDefaultReadOnly(true);
        config2.setDefaultAutoCommit(true);
        config2.setDefaultTransactionIsolationCode(1);
        config2.setDefaultSchema("schema");
        config2.setDefaultCatalog("catalog");

        try (BeeDataSource ds = new BeeDataSource(config)) {
            try (Connection con = ds.getConnection()) {
                Assertions.assertTrue(con.getAutoCommit());
                Assertions.assertFalse(con.isReadOnly());
                Assertions.assertEquals(0, con.getTransactionIsolation());
                Assertions.assertNull(con.getSchema());
                Assertions.assertNull(con.getCatalog());
            }
        }
    }

    @Test
    public void testNotSetDefaultValue() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setPrintRuntimeLogs(true);
        config.setUseDefaultCatalog(true);
        config.setUseDefaultSchema(true);
        config.setUseDefaultReadOnly(true);
        config.setUseDefaultAutoCommit(true);
        config.setUseDefaultTransactionIsolation(true);

        MockConnectionProperties connectionProperties = new MockConnectionProperties();
        connectionProperties.setMockException1(new SQLException("Communication failed"));
        connectionProperties.enableExceptionOnMethod("getAutoCommit,setAutoCommit,isReadOnly,setReadOnly");
        connectionProperties.enableExceptionOnMethod("getTransactionIsolation,setTransactionIsolation");
        connectionProperties.enableExceptionOnMethod("setCatalog,getCatalog,setSchema,getSchema");
        MockConnectionFactory factory = new MockConnectionFactory(connectionProperties);
        config.setConnectionFactory(factory);

        LogCollector logCollector = startLogCollector();
        try (BeeDataSource ds = new BeeDataSource(config)) {
            Assertions.assertNotNull(ds);
        }
        String logs = logCollector.endLogCollector();
        Assertions.assertTrue(logs.contains("failed to get value of auto-commit property"));
        Assertions.assertTrue(logs.contains("failed to get value of transaction-isolation property "));
        Assertions.assertTrue(logs.contains("failed to get value of read-only property"));
        Assertions.assertTrue(logs.contains("failed to get value of catalog property"));
        Assertions.assertTrue(logs.contains("failed to get value of schema property"));
        Assertions.assertTrue(logs.contains("as default value of auto-commit property"));
        Assertions.assertTrue(logs.contains("as default value of transaction-isolation property"));
        Assertions.assertTrue(logs.contains("as default value of read-only property"));


        //not print logs
        config.setPrintRuntimeLogs(false);
        logCollector = startLogCollector();
        try (BeeDataSource ds = new BeeDataSource(config)) {
            Assertions.assertNotNull(ds);
        }
        logs = logCollector.endLogCollector();
        Assertions.assertFalse(logs.contains("failed to get value of auto-commit property"));
        Assertions.assertFalse(logs.contains("failed to get value of transaction-isolation property "));
        Assertions.assertFalse(logs.contains("failed to get value of read-only property"));
        Assertions.assertFalse(logs.contains("failed to get value of catalog property"));
        Assertions.assertFalse(logs.contains("failed to get value of schema property"));
        Assertions.assertFalse(logs.contains("as default value of auto-commit property"));
        Assertions.assertFalse(logs.contains("as default value of transaction-isolation property"));
        Assertions.assertFalse(logs.contains("as default value of read-only property"));
    }

    @Test
    public void testSetDefaultValue() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setPrintRuntimeLogs(true);
        config.setUseDefaultCatalog(true);
        config.setUseDefaultSchema(true);
        config.setUseDefaultReadOnly(true);
        config.setUseDefaultAutoCommit(true);
        config.setUseDefaultTransactionIsolation(true);
        //set defaults to config
        config.setDefaultReadOnly(true);
        config.setDefaultAutoCommit(true);
        config.setDefaultTransactionIsolationCode(1);
        config.setDefaultSchema("schema");
        config.setDefaultCatalog("catalog");

        MockConnectionProperties connectionProperties = new MockConnectionProperties();
        connectionProperties.setMockException1(new SQLException("Communication failed"));
        connectionProperties.enableExceptionOnMethod("getAutoCommit,setAutoCommit,isReadOnly,setReadOnly");
        connectionProperties.enableExceptionOnMethod("getTransactionIsolation,setTransactionIsolation");
        connectionProperties.enableExceptionOnMethod("setCatalog,getCatalog,setSchema,getSchema");
        MockConnectionFactory factory = new MockConnectionFactory(connectionProperties);
        config.setConnectionFactory(factory);


        LogCollector logCollector = startLogCollector();
        try (BeeDataSource ds = new BeeDataSource(config)) {
            Assertions.assertNotNull(ds);
        }
        String logs = logCollector.endLogCollector();
        Assertions.assertTrue(logs.contains("of auto-commit property on first connection object"));
        Assertions.assertTrue(logs.contains("of transaction-isolation property on first connection object"));
        Assertions.assertTrue(logs.contains("of read-only property on first connection object"));
        Assertions.assertTrue(logs.contains("of catalog property on first connection object"));
        Assertions.assertTrue(logs.contains("of schema property on first connection object"));

        //not print logs
        config.setPrintRuntimeLogs(false);
        logCollector = startLogCollector();
        try (BeeDataSource ds = new BeeDataSource(config)) {
            Assertions.assertNotNull(ds);
        }
        logs = logCollector.endLogCollector();
        Assertions.assertFalse(logs.contains("of auto-commit property on first connection object"));
        Assertions.assertFalse(logs.contains("of transaction-isolation property on first connection object"));
        Assertions.assertFalse(logs.contains("of read-only property on first connection object"));
        Assertions.assertFalse(logs.contains("of catalog property on first connection object"));
        Assertions.assertFalse(logs.contains("of schema property on first connection object"));
    }

    @Test
    public void testSupportOnIsValidMethod() throws Exception {
        //return false from isValid
        BeeDataSourceConfig config1 = createDefault();
        config1.setInitialSize(1);
        config1.setPrintRuntimeLogs(true);
        MockConnectionProperties connectionProperties = new MockConnectionProperties();
        connectionProperties.setValid(false);
        MockConnectionFactory factory = new MockConnectionFactory(connectionProperties);
        config1.setConnectionFactory(factory);
        LogCollector logCollector = startLogCollector();
        try (BeeDataSource ds = new BeeDataSource(config1)) {
            Assertions.assertNotNull(ds);
        }
        String logs = logCollector.endLogCollector();
        Assertions.assertTrue(logs.contains("get false from call of isValid method on first connection object"));

        //not print logs
        config1.setPrintRuntimeLogs(false);
        logCollector = startLogCollector();
        try (BeeDataSource ds = new BeeDataSource(config1)) {
            Assertions.assertNotNull(ds);
        }

        logs = logCollector.endLogCollector();
        Assertions.assertFalse(logs.contains("isValid method tested failed on first connection object"));

        //exception from isValid
        BeeDataSourceConfig config2 = createDefault();
        config2.setInitialSize(1);
        config2.setPrintRuntimeLogs(true);
        connectionProperties.setValid(true);
        connectionProperties.enableExceptionOnMethod("isValid");
        connectionProperties.setMockException1(new SQLException("Communication failed"));
        config2.setConnectionFactory(factory);

        logCollector = startLogCollector();
        try (BeeDataSource ds = new BeeDataSource(config2)) {
            Assertions.assertNotNull(ds);
        }
        String logs2 = logCollector.endLogCollector();
        Assertions.assertTrue(logs2.contains("isValid method tested failed on first connection object"));

        //not print logs
        config2.setPrintRuntimeLogs(false);
        logCollector = startLogCollector();
        try (BeeDataSource ds = new BeeDataSource(config2)) {
            Assertions.assertNotNull(ds);
        }
        logs2 = logCollector.endLogCollector();
        Assertions.assertFalse(logs2.contains("isValid method tested failed on first connection object"));
    }

    @Test
    public void testSupportOnNetworkTimeoutMethod() throws Exception {
        BeeDataSourceConfig config = createDefault();
        config.setInitialSize(1);
        config.setPrintRuntimeLogs(true);
        MockConnectionProperties connectionProperties = new MockConnectionProperties();
        connectionProperties.setNetworkTimeout(-1);
        MockConnectionFactory factory = new MockConnectionFactory(connectionProperties);
        config.setConnectionFactory(factory);

        LogCollector logCollector = startLogCollector();
        try (BeeDataSource ds = new BeeDataSource(config)) {
            Assertions.assertNotNull(ds);
        }
        String logs = logCollector.endLogCollector();
        Assertions.assertTrue(logs.contains("networkTimeout property not supported by connections due to a negative number returned from first connection object"));

        //not print logs
        config.setPrintRuntimeLogs(false);
        logCollector = startLogCollector();
        try (BeeDataSource ds = new BeeDataSource(config)) {
            Assertions.assertNotNull(ds);
        }
        logs = logCollector.endLogCollector();
        Assertions.assertFalse(logs.contains("networkTimeout property not supported by connections due to a negative number returned from first connection object"));

        //exception from  getNetworkTimeout
        connectionProperties.setNetworkTimeout(0);
        connectionProperties.enableExceptionOnMethod("getNetworkTimeout");
        connectionProperties.setMockException1(new SQLException("NetworkTimeout"));

        BeeDataSourceConfig config2 = createDefault();
        config2.setInitialSize(1);
        config2.setPrintRuntimeLogs(true);
        config2.setConnectionFactory(factory);
        logCollector = startLogCollector();
        try (BeeDataSource ds = new BeeDataSource(config2)) {
            Assertions.assertNotNull(ds);
        }
        String logs2 = logCollector.endLogCollector();
        Assertions.assertTrue(logs2.contains("networkTimeout property tested failed on first connection object"));

        //not print logs
        config2.setPrintRuntimeLogs(false);
        logCollector = startLogCollector();
        try (BeeDataSource ds = new BeeDataSource(config2)) {
            Assertions.assertNotNull(ds);
        }
        logs2 = logCollector.endLogCollector();
        Assertions.assertFalse(logs2.contains("networkTimeout property tested failed on first connection object"));

        //exception from setNetworkTimeout
        connectionProperties.setNetworkTimeout(10);
        connectionProperties.disableExceptionOnMethod("getNetworkTimeout");
        connectionProperties.enableExceptionOnMethod("setNetworkTimeout");
        BeeDataSourceConfig config3 = createDefault();
        config3.setInitialSize(1);
        config3.setPrintRuntimeLogs(true);
        config3.setConnectionFactory(factory);

        logCollector = startLogCollector();
        try (BeeDataSource ds = new BeeDataSource(config3)) {
            Assertions.assertNotNull(ds);
        }
        String logs3 = logCollector.endLogCollector();
        Assertions.assertTrue(logs3.contains("networkTimeout property tested failed on first connection object"));
    }

}
