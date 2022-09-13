/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.concurrent.synchronizer.impl.bak;

import org.jmin.concurrent.synchronizer.WaitNodeChain;

/**
 * @author Chris Liao
 * @version 1.0
 */
public interface Pool extends WaitNodeChain {

    //true,fair mode to acquire permit
    boolean isFair();

    //true,reentrant
    boolean isReentrant();

    //permit max size in pool
    int getPermitMaxSize();

    //available permit size in pool
    int getPermitSize();

    //release a permit to pool,if hold shared count is greater zero,then reduce its value util zero,then release really
    void release();

    //try to acquire a permit,if interrupted,then ignore it and continue to try
    boolean acquireUninterruptibly(long deadlineNs);

    //try to acquire a permit,if interrupted,then throws InterruptedException
    boolean acquire(long deadlineNs) throws InterruptedException;

}
