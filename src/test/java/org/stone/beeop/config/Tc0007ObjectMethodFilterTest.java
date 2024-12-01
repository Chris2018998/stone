/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beeop.config;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.BeeObjectSourceConfigException;
import org.stone.beeop.objects.JavaBookMethodFilter;
import org.stone.beeop.objects.JavaBookMethodFilter2;

/**
 * @author Chris Liao
 */
public class Tc0007ObjectMethodFilterTest extends TestCase {

    public void testOnAddProperty() {
        BeeObjectSourceConfig config = OsConfigFactory.createEmpty();
        config.setObjectMethodFilterClassName(JavaBookMethodFilter.class.getName());
        Assert.assertEquals(JavaBookMethodFilter.class.getName(), config.getObjectMethodFilterClassName());
        config.setObjectMethodFilterClass(JavaBookMethodFilter.class);
        Assert.assertEquals(JavaBookMethodFilter.class, config.getObjectMethodFilterClass());
        JavaBookMethodFilter filter = new JavaBookMethodFilter();
        config.setObjectMethodFilter(filter);
        Assert.assertEquals(filter, config.getObjectMethodFilter());
    }

    public void testCreation() {
        BeeObjectSourceConfig config = OsConfigFactory.createDefault();
        BeeObjectSourceConfig config2 = config.check();
        Assert.assertNull(config2.getObjectMethodFilter());

        config = OsConfigFactory.createDefault();
        JavaBookMethodFilter filter = new JavaBookMethodFilter();
        config.setObjectMethodFilter(filter);
        config2 = config.check();
        Assert.assertEquals(filter, config2.getObjectMethodFilter());

        config = OsConfigFactory.createDefault();
        config.setObjectMethodFilterClass(JavaBookMethodFilter.class);
        config2 = config.check();
        Assert.assertNotNull(config2.getObjectMethodFilter());

        config = OsConfigFactory.createDefault();
        config.setObjectMethodFilterClassName(JavaBookMethodFilter.class.getName());
        config2 = config.check();
        Assert.assertNotNull(config2.getObjectMethodFilter());

        config = OsConfigFactory.createDefault();
        config.setObjectMethodFilterClassName(JavaBookMethodFilter.class + "Test");
        try {
            config.check();
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Not found object filter class:"));
        }


        config = OsConfigFactory.createDefault();
        config.setObjectMethodFilterClassName(JavaBookMethodFilter2.class.getName());
        try {
            config.check();
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Failed to create object method filter by class:"));
        }
    }
}
