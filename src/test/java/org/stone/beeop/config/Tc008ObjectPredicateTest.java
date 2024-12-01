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
import org.stone.beeop.objects.JavaBookFactory;
import org.stone.beeop.objects.JavaBookPredicate;
import org.stone.beeop.objects.JavaBookPredicate2;

/**
 * @author Chris Liao
 */
public class Tc008ObjectPredicateTest extends TestCase {
    public void testOnAddProperty() {
        BeeObjectSourceConfig config = new BeeObjectSourceConfig();
        config.setObjectPredicateClassName(JavaBookPredicate.class.getName());
        Assert.assertEquals(JavaBookPredicate.class.getName(), config.getObjectPredicateClassName());
        config.setObjectPredicateClass(JavaBookPredicate.class);
        Assert.assertEquals(JavaBookPredicate.class, config.getObjectPredicateClass());
        JavaBookPredicate predicate = new JavaBookPredicate();
        config.setObjectPredicate(predicate);
        Assert.assertEquals(predicate, config.getObjectPredicate());
    }

    public void testCreation() {
        BeeObjectSourceConfig config = new BeeObjectSourceConfig();
        config.setObjectFactory(new JavaBookFactory());
        BeeObjectSourceConfig config2 = config.check();
        Assert.assertNull(config2.getObjectPredicate());

        config = new BeeObjectSourceConfig();
        config.setObjectFactory(new JavaBookFactory());
        JavaBookPredicate predicate = new JavaBookPredicate();
        config.setObjectPredicate(predicate);
        config2 = config.check();
        Assert.assertEquals(predicate, config2.getObjectPredicate());

        config = new BeeObjectSourceConfig();
        config.setObjectFactory(new JavaBookFactory());
        config.setObjectPredicateClass(JavaBookPredicate.class);
        config2 = config.check();
        Assert.assertNotNull(config2.getObjectPredicate());

        config = new BeeObjectSourceConfig();
        config.setObjectFactory(new JavaBookFactory());
        config.setObjectPredicateClassName(JavaBookPredicate.class.getName());
        config2 = config.check();
        Assert.assertNotNull(config2.getObjectPredicate());


        config = new BeeObjectSourceConfig();
        config.setObjectFactory(new JavaBookFactory());
        config.setObjectPredicateClassName(JavaBookPredicate.class + "Test");
        try {
            config.check();
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Not found predicate class"));
        }

        config = new BeeObjectSourceConfig();
        config.setObjectFactory(new JavaBookFactory());
        config.setObjectPredicateClassName(JavaBookPredicate2.class.getName());
        try {
            config.check();
        } catch (BeeObjectSourceConfigException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("Failed to create predicate instance with class"));
        }
    }
}
