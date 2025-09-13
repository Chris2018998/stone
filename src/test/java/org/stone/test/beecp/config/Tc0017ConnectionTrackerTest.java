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
import org.stone.beecp.BeeConnectionTracker;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.test.beecp.objects.MockCommonConnectionFactory;
import org.stone.test.beecp.objects.MockConnectionTracker;
import org.stone.tools.exception.BeanException;

import static org.stone.test.beecp.config.DsConfigFactory.createEmpty;

/**
 * @author Chris Liao
 */

public class Tc0017ConnectionTrackerTest {

    @Test
    public void testOnSetGet() {
        BeeDataSourceConfig config = createEmpty();
        Class<? extends BeeConnectionTracker> trackerClass = MockConnectionTracker.class;
        config.setConnectionTrackerClass(trackerClass);
        Assertions.assertEquals(trackerClass, config.getConnectionTrackerClass());

        String trackerClassName = MockConnectionTracker.class.getName();
        config.setConnectionTrackerClassName(trackerClassName);
        Assertions.assertEquals(trackerClassName, config.getConnectionTrackerClassName());

        MockConnectionTracker tracker = new MockConnectionTracker();
        config.setConnectionTracker(tracker);
        Assertions.assertEquals(config.getConnectionTracker(), tracker);
    }

    @Test
    public void testErrorTrackerClassName() throws Exception {
        BeeDataSourceConfig config1 = createEmpty();
        MockCommonConnectionFactory connectionFactory = new MockCommonConnectionFactory();
        config1.setConnectionFactory(connectionFactory);
        config1.setConnectionTrackerClassName("org.stone.test.beecp.objects.MockConnectionTracker3");//class not found
        try {
            config1.check();
            Assertions.fail();
        } catch (BeeDataSourceConfigException e) {
            Assertions.assertInstanceOf(ClassNotFoundException.class, e.getCause());
        }

        BeeDataSourceConfig config2 = createEmpty();
        config2.setConnectionFactory(connectionFactory);
        config2.setConnectionTrackerClassName("org.stone.test.beecp.objects.MockConnectionTracker2");//class can not be instan
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
