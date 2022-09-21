/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.stone.synchronizer;

/**
 * @author Chris Liao
 * @version 1.0
 * <p>
 * design for Lock(ReentrantLock/ReentrantReadWriteLock)
 */
public interface SharableObject {

    //true,fair mode acquisition
    boolean isFair();

    //true,if hold by current thread
    boolean isHeldByCurrentThread();

    //true,if hold with shared mode by current thread
    boolean isSharedHeldByCurrentThread();

    //true,if hold with exclusive mode by current thread
    boolean isExclusiveHeldByCurrentThread();

    //return current thread hold count(reentrant)
    int getHoldCountByCurrentThread();

    //Threads waiting for shared lock
    Thread[] getQueuedSharedThreads();

    //Threads waiting for exclusive lock
    Thread[] getQueuedExclusiveThreads();

    //release lock to pool,if hold shared count is greater zero,then reduce its value util zero,then release really
    void release(boolean isExclusive);

    //try to acquire lock,if interrupted,then ignore it and continue to try
    boolean acquireUninterruptibly(boolean exclusive, long deadlineNs);

    //try to acquire lock,if interrupted,then throws InterruptedException
    boolean acquire(boolean exclusive, long deadlineNs) throws InterruptedException;
}
