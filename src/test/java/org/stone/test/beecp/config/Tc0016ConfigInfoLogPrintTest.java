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
import org.stone.test.base.LogCollector;

import static org.stone.test.base.LogCollector.startLogCollector;
import static org.stone.test.beecp.config.DsConfigFactory.createDefault;

/**
 * @author Chris Liao
 */
public class Tc0016ConfigInfoLogPrintTest {

    @Test
    public void testConfigurationSet() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        Assertions.assertFalse(config.isPrintConfiguration());//default check
        config.setPrintConfiguration(true);
        Assertions.assertTrue(config.isPrintConfiguration());
        config.setPrintConfiguration(false);
        Assertions.assertFalse(config.isPrintConfiguration());
    }

    @Test
    public void testOnConfigPrintInd() throws Exception {
        BeeDataSourceConfig config = createDefault();
        //situation1: not print config
        config.setPrintConfiguration(false);//test point

        LogCollector logCollector = startLogCollector();
        config.check();
        String logs = logCollector.endLogCollector();
        Assertions.assertTrue(logs.isEmpty());

        //situation2: print config items
        config.setPrintConfiguration(true);//test point
        logCollector = startLogCollector();
        config.check();
        logs = logCollector.endLogCollector();
        Assertions.assertFalse(logs.isEmpty());
    }

    @Test
    public void testOnExclusionConfigItems() throws Exception {

        BeeDataSourceConfig config = createDefault();
        config.setPrintConfiguration(true);

        //situation1: check default exclusion print
        LogCollector logCollector = startLogCollector();
        config.check();
        String logs = logCollector.endLogCollector();
        Assertions.assertFalse(logs.contains(".username"));
        Assertions.assertFalse(logs.contains(".password"));
        Assertions.assertFalse(logs.contains(".jdbcUrl"));
        Assertions.assertFalse(logs.contains(".user"));
        Assertions.assertFalse(logs.contains(".url"));

        //situation2: print out all items(clear all exclusion)
        config.clearAllConfigPrintExclusion();//test point
        logCollector = startLogCollector();
        config.check();
        logs = logCollector.endLogCollector();
        Assertions.assertTrue(logs.contains(".username"));
        Assertions.assertTrue(logs.contains(".password"));
        Assertions.assertTrue(logs.contains(".jdbcUrl"));

        //situation3: test on a not excluded item
        Assertions.assertTrue(logs.contains(".maxActive"));

        //situation4: test on excluded item
        config.addConfigPrintExclusion("maxActive");//test point
        logCollector = startLogCollector();
        config.check();
        logs = logCollector.endLogCollector();
        Assertions.assertFalse(logs.contains(".maxActive"));

        //situation5: test on pint connectProperties
        config.addConnectProperty("dbGroup", "test");
        config.addConnectProperty("dbName", "test-Mysql1");

        logCollector = startLogCollector();
        config.check();
        logs = logCollector.endLogCollector();
        Assertions.assertTrue(logs.contains(".connectProperties.dbGroup"));
        Assertions.assertTrue(logs.contains(".connectProperties.dbName"));

        //situation6: test on exclusion connectProperties
        config.addConfigPrintExclusion("dbGroup");
        config.addConfigPrintExclusion("dbName");
        config.addConfigPrintExclusion("connectProperties");

        logCollector = startLogCollector();
        config.check();
        logs = logCollector.endLogCollector();
        Assertions.assertFalse(logs.contains(".connectProperties.dbGroup"));
        Assertions.assertFalse(logs.contains(".connectProperties.dbName"));
    }
}
