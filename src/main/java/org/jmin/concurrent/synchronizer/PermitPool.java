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
 * design for Semaphore
 */
public interface PermitPool {

    //true,fair mode to acquire permit
    boolean isFair();

    //permit max size in pool
    int getMaxSize();

    //available permit size in pool
    int getSize();

    //release a permit to pool,if hold shared count is greater zero,then reduce its value util zero,then release really
    void release();

    //try to acquire a permit,if interrupted,then ignore it and continue to try
    boolean acquireUninterruptibly(long deadlineNs);

    //try to acquire a permit,if interrupted,then throws InterruptedException
    boolean acquire(long deadlineNs) throws InterruptedException;
}
