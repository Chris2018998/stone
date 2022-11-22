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

import org.stone.shine.synchronizer.locks.ReentrantLock;

import java.util.concurrent.locks.Lock;

/**
 * lock factory impl
 *
 * @author Chris Liao
 * @version 1.0
 */

public class ReentrantLockFactory extends ConditionLockFactory {

    public Lock createLock(boolean fair) {
        return new ReentrantLock(fair);
    }
}
