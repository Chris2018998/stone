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

import org.stone.base.TestCase;
import org.stone.shine.util.concurrent.locks.ReentrantLock;

import java.util.concurrent.locks.Condition;

/**
 * ReentrantLock condition test
 *
 * @author Chris Liao
 * @version 1.0
 */
public abstract class ReentrantLockConditionTestCase extends TestCase {
    protected ReentrantLock lock;
    protected Condition lockCondition;

    public void setUp() throws Throwable {
        this.lock = new ReentrantLock();
        this.lockCondition = lock.newCondition();
    }
}
