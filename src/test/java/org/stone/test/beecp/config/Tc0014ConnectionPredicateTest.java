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
import org.stone.test.beecp.objects.MockCommonConnectionFactory;
import org.stone.test.beecp.objects.MockEvictConnectionPredicate;
import org.stone.tools.exception.BeanException;

import static org.stone.test.beecp.config.DsConfigFactory.createEmpty;

/**
 * @author Chris Liao
 */
public class Tc0014ConnectionPredicateTest {

    @Test
    public void testConfigurationSet() {
        BeeDataSourceConfig config = new BeeDataSourceConfig();

        Assertions.assertNull(config.getEvictPredicate());//default check
        config.setEvictPredicate(new MockEvictConnectionPredicate());
        Assertions.assertNotNull(config.getEvictPredicate());//default check
        config.setEvictPredicate(null);
        Assertions.assertNull(config.getEvictPredicate());

        Assertions.assertNull(config.getEvictPredicateClass());//default check
        config.setEvictPredicateClass(MockEvictConnectionPredicate.class);
        Assertions.assertNotNull(config.getEvictPredicateClass());
        config.setEvictPredicateClass(null);
        Assertions.assertNull(config.getEvictPredicateClass());

        Assertions.assertNull(config.getEvictPredicateClassName());//default check
        config.setEvictPredicateClassName(MockEvictConnectionPredicate.class.getName());
        Assertions.assertNotNull(config.getEvictPredicateClassName());
        config.setEvictPredicateClassName(null);
        Assertions.assertNull(config.getEvictPredicateClassName());
    }

    @Test
    public void testErrorClassName() throws Exception {
        MockCommonConnectionFactory connectionFactory = new MockCommonConnectionFactory();
        BeeDataSourceConfig config1 = createEmpty();
        config1.setConnectionFactory(connectionFactory);
        config1.setEvictPredicateClassName("org.stone.test.beecp.objects.MockEvictConnectionPredicate2");//class can not be
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
        config2.setEvictPredicateClassName("org.stone.test.beecp.objects.MockEvictConnectionPredicate3");//class not found
        try {
            config2.check();
            Assertions.fail();
        } catch (BeeDataSourceConfigException e) {
            Assertions.assertInstanceOf(ClassNotFoundException.class, e.getCause());
        }
    }
}
