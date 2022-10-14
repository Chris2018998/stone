/*
 * Copyright(C) Chris2018998(cn)
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.stone.synchronizer;

import java.util.Collection;

/**
 * @author Chris Liao
 * @version 1.0
 */
public interface ResourceAccess extends Acquirable {

    //true,if hold by current thread
    boolean isHeldByCurrentThread();

    //return current thread hold count(reentrant)
    int getHoldCountByCurrentThread();

    //true,if hold with shared mode by current thread
    boolean isSharedHeldByCurrentThread();

    //true,if hold with exclusive mode by current thread
    boolean isExclusiveHeldByCurrentThread();

    //Threads waiting for shared lock
    Collection<Thread> getQueuedSharedThreads();

    //Threads waiting for exclusive lock
    Collection<Thread> getQueuedExclusiveThreads();

    //release lock to pool,if hold shared count is greater zero,then reduce its value util zero,then release really
    void release(boolean isExclusive);

    //try to acquire lock,if interrupted,then ignore it and continue to try
    boolean acquireUninterruptibly(boolean exclusive, long deadlineNs);

    //try to acquire lock,if interrupted,then throws InterruptedException
    boolean acquire(boolean exclusive, long deadlineNs) throws InterruptedException;
}
