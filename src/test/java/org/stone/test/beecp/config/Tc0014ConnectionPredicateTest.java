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
import org.stone.test.beecp.objects.eviction.MockEvictConnectionPredicate;
import org.stone.test.beecp.objects.factory.MockConnectionFactory;
import org.stone.tools.exception.BeanException;

import static org.stone.test.beecp.config.DsConfigFactory.createEmpty;

/**
 * @author Chris Liao
 */
public class Tc0014ConnectionPredicateTest {

    @Test
    public void testConfigurationSet() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();

        Assertions.assertNull(config.getPredicate());//default check
        config.setPredicate(new MockEvictConnectionPredicate());
        Assertions.assertNotNull(config.getPredicate());//default check
        config.setPredicate(null);
        Assertions.assertNull(config.getPredicate());

        Assertions.assertNull(config.getPredicateClass());//default check
        config.setPredicateClass(MockEvictConnectionPredicate.class);
        Assertions.assertNotNull(config.getPredicateClass());
        config.setPredicateClass(null);
        Assertions.assertNull(config.getPredicateClass());

        Assertions.assertNull(config.getPredicateClassName());//default check
        config.setPredicateClassName(MockEvictConnectionPredicate.class.getName());
        Assertions.assertNotNull(config.getPredicateClassName());
        config.setPredicateClassName(null);
        Assertions.assertNull(config.getPredicateClassName());
    }

    @Test
    public void testErrorClassName() throws Exception {
        MockConnectionFactory connectionFactory = new MockConnectionFactory();
        BeeDataSourceConfig config1 = createEmpty();
        config1.setConnectionFactory(connectionFactory);
        config1.setPredicateClassName("org.stone.test.beecp.objects.eviction.MockEvictConnectionPredicate2");//class can not be
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
        config2.setPredicateClassName("org.stone.test.beecp.objects.MockEvictConnectionPredicate3");//class not found
        try {
            config2.check();
            Assertions.fail();
        } catch (BeeDataSourceConfigException e) {
            Assertions.assertInstanceOf(ClassNotFoundException.class, e.getCause());
        }
    }
}
