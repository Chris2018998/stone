/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.stone.synchronizer.impl;

import org.jmin.stone.synchronizer.SharableObject;

/**
 * @author Chris Liao
 * @version 1.0
 */

public class SynchronizedObject implements SharableObject {

    //true,fair mode acquisition
    public boolean isFair() {
        return false;
    }

    //true,if hold by current thread
    public boolean isHeldByCurrentThread() {
        return false;
    }

    //true,if hold with shared mode by current thread
    public boolean isSharedHeldByCurrentThread() {
        return false;
    }

    //true,if hold with exclusive mode by current thread
    public boolean isExclusiveHeldByCurrentThread() {
        return false;
    }

    //return current thread hold count(reentrant)
    public int getHoldCountByCurrentThread() {
        return 1;
    }

    //release a permit to pool,if hold shared count is greater zero,then reduce its value util zero,then release really
    public void release(boolean isExclusive) {

    }

    //try to acquire a permit,if interrupted,then ignore it and continue to try
    public boolean acquireUninterruptibly(boolean exclusive, long deadlineNs) {
        return false;
    }

    //try to acquire a permit,if interrupted,then throws InterruptedException
    public boolean acquire(boolean exclusive, long deadlineNs) throws InterruptedException {
        return false;
    }
}
