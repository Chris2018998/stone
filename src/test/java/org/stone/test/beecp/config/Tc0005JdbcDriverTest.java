/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.test.beecp.driver.MockDriver;

import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.stone.test.beecp.config.DsConfigFactory.*;

/**
 * @author Chris Liao
 */

public class Tc0005JdbcDriverTest {

    @Test
    public void testUrlMatchDriver() throws SQLException {
        //url match format: jdbc:beecp:xxx
        DriverManager.registerDriver(new MockDriver());

        BeeDataSourceConfig config = createEmpty();
        try {//1: test not found matched driver with url
            config.setUrl("jdbc:beecp1://localhost/testdb");
            config.check();
            fail("[testUrlMatchDriver]Test failed");
        } catch (SQLException e) {//thrown from DriverManager
            String message = e.getMessage();
            assertTrue(message != null && message.contains("No suitable driver"));
        }

        //2: set a matched url and recheck
        config.setUrl("jdbc:beecp://localhost/testdb");
        Assertions.assertNotNull(config.check());//
    }

    @Test
    public void testConfigDriverNotMatchUrl() throws Exception {
        try {
            BeeDataSourceConfig config = createEmpty();
            config.setJdbcUrl("Test:" + JDBC_URL);
            config.setDriverClassName(JDBC_DRIVER);
            config.check();
            fail("[testConfigDriverNotMatchUrl]Not threw exception when url not matched driver");
        } catch (BeeDataSourceConfigException e) {//thrown from Config.check()
            String message = e.getMessage();
            assertTrue(message != null && message.contains("can not match configured driver"));
        }
    }
}
