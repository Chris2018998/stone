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
import org.stone.shine.synchronizer.locks.ReentrantReadWriteLock;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * writeLock condition test
 *
 * @author Chris Liao
 * @version 1.0
 */
public abstract class WriteLockConditionTestCase extends TestCase {
    protected Lock lock;
    protected Condition lockCondition;

    public void setUp() throws Throwable {
        this.lock = new ReentrantReadWriteLock().writeLock();
        this.lockCondition = lock.newCondition();
    }
}
