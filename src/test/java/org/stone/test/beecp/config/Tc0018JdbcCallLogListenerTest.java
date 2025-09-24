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
import org.stone.beecp.BeeJdbcCallLogCollector;
import org.stone.beecp.BeeJdbcCallLogListener;
import org.stone.test.beecp.objects.MockCommonConnectionFactory;
import org.stone.test.beecp.objects.MockJdbcCallLogCollector;
import org.stone.test.beecp.objects.MockJdbcCallLogListener;
import org.stone.tools.exception.BeanException;

import static org.stone.test.beecp.config.DsConfigFactory.createEmpty;

/**
 * @author Chris Liao
 */

public class Tc0018JdbcCallLogListenerTest {

    @Test
    public void testOnSetGet() {
        BeeDataSourceConfig config = createEmpty();
        Class<? extends BeeJdbcCallLogListener> listenerClass = MockJdbcCallLogListener.class;
        config.setJdbcCallLogListenerClass(listenerClass);
        Assertions.assertEquals(listenerClass, config.getJdbcCallLogListenerClass());

        String listenerClassName = MockJdbcCallLogListener.class.getName();
        config.setJdbcCallLogListenerClassName(listenerClassName);
        Assertions.assertEquals(listenerClassName, config.getJdbcCallLogListenerClassName());

        MockJdbcCallLogListener listener = new MockJdbcCallLogListener();
        config.setJdbcCallLogListener(listener);
        Assertions.assertEquals(config.getJdbcCallLogListener(), listener);
    }

    @Test
    public void testErrorClassName() throws Exception {
        BeeDataSourceConfig config1 = createEmpty();
        MockCommonConnectionFactory connectionFactory = new MockCommonConnectionFactory();
        config1.setConnectionFactory(connectionFactory);
        BeeJdbcCallLogCollector logCollector = new MockJdbcCallLogCollector();
        config1.setJdbcCallLogCollector(logCollector);
        config1.setJdbcCallLogListenerClassName("org.stone.test.beecp.objects.MockJdbcCallLogListener3");//class not found
        try {
            config1.check();
            Assertions.fail();
        } catch (BeeDataSourceConfigException e) {
            Assertions.assertInstanceOf(ClassNotFoundException.class, e.getCause());
        }

        BeeDataSourceConfig config2 = createEmpty();
        config2.setConnectionFactory(connectionFactory);
        config2.setJdbcCallLogCollector(logCollector);
        config2.setJdbcCallLogListenerClassName("org.stone.test.beecp.objects.MockJdbcCallLogListener2");//class can not be
        try {
            config2.check();
            Assertions.fail();
        } catch (BeeDataSourceConfigException e) {
            Throwable cause1 = e.getCause();
            Assertions.assertInstanceOf(BeanException.class, cause1);
            Assertions.assertInstanceOf(NoSuchMethodException.class, cause1.getCause());
        }

        BeeDataSourceConfig config3 = createEmpty();
        config3.setConnectionFactory(connectionFactory);
        config3.setJdbcCallLogCollector(logCollector);
        config3.setJdbcCallLogListener(new org.stone.test.beecp.objects.MockJdbcCallLogListener());
        BeeDataSourceConfig config31 = config3.check();
        Assertions.assertNotNull(config31.getJdbcCallLogListener());


        BeeDataSourceConfig config4 = createEmpty();
        config4.setConnectionFactory(connectionFactory);
        config4.setJdbcCallLogCollector(logCollector);
        config4.setJdbcCallLogListenerClass(org.stone.test.beecp.objects.MockJdbcCallLogListener.class);
        BeeDataSourceConfig config41 = config4.check();
        Assertions.assertNotNull(config41.getJdbcCallLogListener());

        BeeDataSourceConfig config5 = createEmpty();
        config5.setJdbcCallLogCollector(logCollector);
        config5.setConnectionFactory(connectionFactory);
        config5.setJdbcCallLogListenerClassName(org.stone.test.beecp.objects.MockJdbcCallLogListener.class.getName());
        BeeDataSourceConfig config51 = config5.check();
        Assertions.assertNotNull(config51.getJdbcCallLogListener());
    }
}
