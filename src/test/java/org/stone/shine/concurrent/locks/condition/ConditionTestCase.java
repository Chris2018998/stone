/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.concurrent.locks.condition;

import org.stone.test.TestCase;

/**
 * lock condition test
 *
 * @author Chris Liao
 * @version 1.0
 */
public abstract class ConditionTestCase extends TestCase {

    protected String lockName;

    protected ConditionLockFactory lockFactory;

    public void setLockFactory(String lockName, ConditionLockFactory lockFactory) {
        this.lockName = lockName;
        this.lockFactory = lockFactory;
    }
}
