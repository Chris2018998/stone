/*
 * Copyright(C) Chris2018998(cn)
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.stone.synchronizer;

/**
 * @author Chris Liao
 * @version 1.0
 */
public interface PermitPool {

    //true,fair mode acquisition
    boolean isFair();

    //available permit size in pool
    int getPermitSize();

    //Threads waiting for permit
    Thread[] getQueuedThreads();

    //release a permit to pool
    void release();

    //try to acquire a permit,if interrupted,then ignore it and continue to try
    boolean acquireUninterruptibly(long deadlineNs);

    //try to acquire a permit,if interrupted,then throws InterruptedException
    boolean acquire(long deadlineNs) throws InterruptedException;

}
