/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.concurrent.synchronizer;

/**
 * @author Chris Liao
 * @version 1.0
 * <p>
 * design for Lock(ReentrantLock/ReentrantReadWriteLock)
 */
public interface MutexState {

    boolean isFair();

    //current state
    int getState();

    //try to acquire a permit,if interrupted,then throws InterruptedException
    void release(int state);

    //try to acquire a permit,if interrupted,then ignore it and continue to try
    boolean acquireUninterruptibly(int state, long deadlineNs);

    //try to acquire a permit,if interrupted,then throws InterruptedException
    boolean acquire(int state, long deadlineNs) throws InterruptedException;

}
