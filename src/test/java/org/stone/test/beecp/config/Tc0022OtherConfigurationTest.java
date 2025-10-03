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

/**
 * @author Chris Liao
 */
public class Tc0022OtherConfigurationTest {

    @Test
    public void testConfigurationSet() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        //fairMode
        Assertions.assertFalse(config.isFairMode());//default is false
        config.setFairMode(true);
        Assertions.assertTrue(config.isFairMode());

        //asyncCreateInitConnection
        Assertions.assertFalse(config.isAsyncCreateInitConnection());//default is false
        config.setAsyncCreateInitConnection(true);
        Assertions.assertTrue(config.isAsyncCreateInitConnection());

        //enableJmx
        Assertions.assertFalse(config.isEnableJmx());//default check
        config.setEnableJmx(true);
        Assertions.assertTrue(config.isEnableJmx());
        config.setEnableJmx(false);
        Assertions.assertFalse(config.isEnableJmx());

        //enableThreadLocal
        Assertions.assertTrue(config.isEnableThreadLocal());//default check
        config.setEnableThreadLocal(false);
        Assertions.assertFalse(config.isEnableThreadLocal());
        config.setEnableThreadLocal(true);
        Assertions.assertTrue(config.isEnableThreadLocal());
    }
}
