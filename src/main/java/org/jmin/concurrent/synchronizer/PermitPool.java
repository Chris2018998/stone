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
 */
public interface PermitPool extends PermitWaitNodeChain {

    //true,fair mode to acquire synchronizer
    boolean isFair();

    //true,reentrant
    boolean isReentrant();

    //synchronizer max size in pool
    int getPermitMaxSize();

    //available synchronizer size in pool
    int getPermitSize();

    //release a synchronizer to pool,if hold shared count is greater zero,then reduce its value util zero,then release really
    boolean release();

    //if <parameter>acquiredShare</parameter> is true,then try to acquire synchronizer,then share to same acquired others
    boolean acquireUninterruptibly(long deadlineNs);

    //acquire a synchronizer and can throws InterruptedException during acquiring
    boolean acquire(long deadlineNs) throws InterruptedException;

}
