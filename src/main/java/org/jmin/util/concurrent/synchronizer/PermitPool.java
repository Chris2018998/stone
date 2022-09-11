/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.util.concurrent.synchronizer;

/**
 * @author Chris Liao
 * @version 1.0
 */
public interface PermitPool extends PermitWaitNodeChain {

    //true,fair mode to acquire permit
    boolean isFair();

    //available permit size in pool
    int getPermitSize();

    //acquired count with share mode(shareAcquire==true)
    int getSharedCount();

    //permit acquired by shared mode then return true
    boolean isInShared();

    //release a permit to pool
    boolean release();

    boolean acquireUninterruptibly(boolean shareAcquire, long deadlineNs);

    boolean acquire(boolean shareAcquire, long deadlineNs) throws InterruptedException;

}
