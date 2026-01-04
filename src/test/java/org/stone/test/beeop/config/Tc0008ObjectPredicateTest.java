/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beeop.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stone.beeop.BeeObjectSourceConfig;
import org.stone.beeop.exception.BeeObjectSourceConfigException;
import org.stone.test.beeop.objects.JavaBookPredicate;
import org.stone.test.beeop.objects.JavaBookPredicate2;

/**
 * @author Chris Liao
 */
public class Tc0008ObjectPredicateTest {
    @Test
    public void testOnAddProperty() {
        BeeObjectSourceConfig config = OsConfigFactory.createEmpty();
        config.setPredicateClassName(JavaBookPredicate.class.getName());
        Assertions.assertEquals(JavaBookPredicate.class.getName(), config.getPredicateClassName());
        config.setPredicateClass(JavaBookPredicate.class);
        Assertions.assertEquals(JavaBookPredicate.class, config.getPredicateClass());
        JavaBookPredicate predicate = new JavaBookPredicate();
        config.setPredicate(predicate);
        Assertions.assertEquals(predicate, config.getPredicate());
    }

    @Test
    public void testCreation() {
        BeeObjectSourceConfig config = OsConfigFactory.createDefault();
        BeeObjectSourceConfig config2 = config.check();
        Assertions.assertNull(config2.getPredicate());

        config = OsConfigFactory.createDefault();
        JavaBookPredicate predicate = new JavaBookPredicate();
        config.setPredicate(predicate);
        config2 = config.check();
        Assertions.assertEquals(predicate, config2.getPredicate());

        config = OsConfigFactory.createDefault();
        config.setPredicateClass(JavaBookPredicate.class);
        config2 = config.check();
        Assertions.assertNotNull(config2.getPredicate());

        config = OsConfigFactory.createDefault();
        config.setPredicateClassName(JavaBookPredicate.class.getName());
        config2 = config.check();
        Assertions.assertNotNull(config2.getPredicate());

        config = OsConfigFactory.createDefault();
        config.setPredicateClassName(JavaBookPredicate.class + "Test");
        try {
            config.check();
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Not found predicate class"));
        }

        config = OsConfigFactory.createDefault();
        config.setPredicateClassName(JavaBookPredicate2.class.getName());
        try {
            config.check();
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assertions.assertTrue(message != null && message.contains("Failed to create predicate instance with class"));
        }
    }
}
