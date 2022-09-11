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

    //permit max size in pool
    int getPermitMaxSize();

    //available permit size in pool
    int getPermitSize();

    //acquired count with share mode(shareAcquire==true)
    int getSharedCount();

    //permit acquired by shared mode then return true
    boolean hasSharedPermit();

    //get hold shared count for current thread
    int getHoldSharedCount();

    //get hold exclusive count for current thread
    int getHoldExclusiveCount();

    //plugin method after permit acquired successful
    void afterAcquired(boolean shareAcquired);

    //plugin method after permit released successful
    void afterReleased(boolean shareAcquired);

    //release a permit to pool
    boolean release();

    //if <parameter>acquiredShare</parameter> is true,then try to acquire permit,then share to same acquired others
    boolean acquireUninterruptibly(boolean shareAcquired, long deadlineNs);

    //acquire a permit and can throws InterruptedException during acquiring
    boolean acquire(boolean shareAcquired, long deadlineNs) throws InterruptedException;

}
