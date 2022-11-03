/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.concurrent.locks;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * @author Chris Liao
 * @version 1.0
 */
public class ReentrantReadWriteLock2 implements ReadWriteLock {

    /**
     * Returns the lock used for reading.
     *
     * @return the lock used for reading
     */
    public Lock readLock() {
        //@todo
        return null;
    }

    /**
     * Returns the lock used for writing.
     *
     * @return the lock used for writing
     */
    public Lock writeLock() {
        //@todo
        return null;
    }
}
