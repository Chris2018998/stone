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
import org.stone.beecp.BeeConnectionInterceptor;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.test.beecp.objects.MockCommonConnectionFactory;
import org.stone.test.beecp.objects.MockConnectionInterceptor;
import org.stone.tools.exception.BeanException;

import static org.stone.test.beecp.config.DsConfigFactory.createEmpty;

/**
 * @author Chris Liao
 */

public class Tc0017ConnectionInterceptorTest {

    @Test
    public void testOnSetGet() {
        BeeDataSourceConfig config = createEmpty();
        Class<? extends BeeConnectionInterceptor> trackerClass = MockConnectionInterceptor.class;
        config.setConnectionInterceptorClass(trackerClass);
        Assertions.assertEquals(trackerClass, config.getConnectionInterceptorClass());

        String trackerClassName = MockConnectionInterceptor.class.getName();
        config.setConnectionInterceptorClassName(trackerClassName);
        Assertions.assertEquals(trackerClassName, config.getConnectionInterceptorClassName());

        MockConnectionInterceptor tracker = new MockConnectionInterceptor();
        config.setConnectionInterceptor(tracker);
        Assertions.assertEquals(config.getConnectionInterceptor(), tracker);
    }

    @Test
    public void testErrorTrackerClassName() throws Exception {
        BeeDataSourceConfig config1 = createEmpty();
        MockCommonConnectionFactory connectionFactory = new MockCommonConnectionFactory();
        config1.setConnectionFactory(connectionFactory);
        config1.setConnectionInterceptorClassName("org.stone.test.beecp.objects.MockConnectionInterceptor3");//class not found
        try {
            config1.check();
            Assertions.fail();
        } catch (BeeDataSourceConfigException e) {
            Assertions.assertInstanceOf(ClassNotFoundException.class, e.getCause());
        }

        BeeDataSourceConfig config2 = createEmpty();
        config2.setConnectionFactory(connectionFactory);
        config2.setConnectionInterceptorClassName("org.stone.test.beecp.objects.MockConnectionInterceptor2");//class can not be instan
        try {
            config2.check();
            Assertions.fail();
        } catch (BeeDataSourceConfigException e) {
            Throwable cause1 = e.getCause();
            Assertions.assertInstanceOf(BeanException.class, cause1);
            Assertions.assertInstanceOf(NoSuchMethodException.class, cause1.getCause());
        }
    }
}
