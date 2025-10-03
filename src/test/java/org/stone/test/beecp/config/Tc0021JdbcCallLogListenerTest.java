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
import org.stone.beecp.BeeConnectionFactory;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.BeeJdbcCallLogCollector;
import org.stone.test.beecp.objects.MockCommonConnectionFactory;
import org.stone.test.beecp.objects.MockJdbcCallLogCollector;
import org.stone.test.beecp.objects.MockJdbcCallLogListener;
import org.stone.tools.exception.BeanException;

import static org.stone.test.beecp.config.DsConfigFactory.createEmpty;

/**
 * @author Chris Liao
 */

public class Tc0021JdbcCallLogListenerTest {

    @Test
    public void testConfigurationSet() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();
        Assertions.assertTrue(config.isJdbcCallLogListenInSync());//default check
        config.setJdbcCallLogListenInSync(false);
        Assertions.assertFalse(config.isJdbcCallLogListenInSync());

        Assertions.assertNull(config.getJdbcCallLogListener());//default check
        config.setJdbcCallLogListener(new MockJdbcCallLogListener());
        Assertions.assertNotNull(config.getJdbcCallLogListener());//default check
        config.setJdbcCallLogListener(null);
        Assertions.assertNull(config.getJdbcCallLogListener());//default check

        Assertions.assertNull(config.getJdbcCallLogListenerClass());//default check
        config.setJdbcCallLogListenerClass(MockJdbcCallLogListener.class);
        Assertions.assertNotNull(config.getJdbcCallLogListenerClass());
        config.setJdbcCallLogListenerClass(null);
        Assertions.assertNull(config.getJdbcCallLogListenerClass());

        Assertions.assertNull(config.getJdbcCallLogListenerClassName());//default check
        config.setJdbcCallLogListenerClassName(MockJdbcCallLogListener.class.getName());
        Assertions.assertNotNull(config.getJdbcCallLogListenerClassName());
        config.setJdbcCallLogListenerClassName(null);
        Assertions.assertNull(config.getJdbcCallLogListenerClassName());
    }

    @Test
    public void testWithoutLogCollector() throws Exception {
        BeeDataSourceConfig config1 = createEmpty();
        BeeConnectionFactory connectionFactory = new MockCommonConnectionFactory();
        config1.setConnectionFactory(connectionFactory);
        config1.setJdbcCallLogListener(new MockJdbcCallLogListener());
        BeeDataSourceConfig config11 = config1.check();
        Assertions.assertNull(config11.getJdbcCallLogListener());

        BeeDataSourceConfig config2 = createEmpty();
        config2.setConnectionFactory(connectionFactory);
        config2.setJdbcCallLogListenerClass(MockJdbcCallLogListener.class);
        BeeDataSourceConfig config21 = config2.check();
        Assertions.assertNull(config21.getJdbcCallLogListener());

        BeeDataSourceConfig config3 = createEmpty();
        config3.setConnectionFactory(connectionFactory);
        config3.setJdbcCallLogListenerClassName(MockJdbcCallLogListener.class.getName());
        BeeDataSourceConfig config31 = config3.check();
        Assertions.assertNull(config31.getJdbcCallLogListener());

        //test set a log collector to config object
        BeeJdbcCallLogCollector logCollector = new MockJdbcCallLogCollector();
        config1.setJdbcCallLogCollector(logCollector);
        config11 = config1.check();
        Assertions.assertNotNull(config11.getJdbcCallLogListener());

        config2.setJdbcCallLogCollector(logCollector);
        config21 = config2.check();
        Assertions.assertNotNull(config21.getJdbcCallLogListener());

        config3.setJdbcCallLogCollector(logCollector);
        config31 = config3.check();
        Assertions.assertNotNull(config31.getJdbcCallLogListener());
    }

    @Test
    public void testErrorClassName() throws Exception {
        BeeJdbcCallLogCollector logCollector = new MockJdbcCallLogCollector();
        MockCommonConnectionFactory connectionFactory = new MockCommonConnectionFactory();
        BeeDataSourceConfig config1 = createEmpty();
        config1.setConnectionFactory(connectionFactory);
        config1.setJdbcCallLogCollector(logCollector);
        config1.setJdbcCallLogListenerClassName("org.stone.test.beecp.objects.MockJdbcCallLogListener2");//class can not be
        try {
            config1.check();
            Assertions.fail();
        } catch (BeeDataSourceConfigException e) {
            Throwable cause1 = e.getCause();
            Assertions.assertInstanceOf(BeanException.class, cause1);
            Assertions.assertInstanceOf(NoSuchMethodException.class, cause1.getCause());
        }

        BeeDataSourceConfig config2 = createEmpty();
        config2.setConnectionFactory(connectionFactory);
        config2.setJdbcCallLogCollector(logCollector);
        config2.setJdbcCallLogListenerClassName("org.stone.test.beecp.objects.MockJdbcCallLogListener3");//class not found
        try {
            config2.check();
            Assertions.fail();
        } catch (BeeDataSourceConfigException e) {
            Assertions.assertInstanceOf(ClassNotFoundException.class, e.getCause());
        }


    }
}
