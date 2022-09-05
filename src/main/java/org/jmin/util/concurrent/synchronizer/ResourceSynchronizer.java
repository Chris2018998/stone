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

public interface ResourceSynchronizer extends SynchronizeNodeChain {

    //***************************************************************************************************************//
    //                                           1: resource state/size                                              //
    //***************************************************************************************************************//
    int getState();

    void setState(int newState);

    boolean compareAndSetState(int expect, int update);

    //***************************************************************************************************************//
    //                                           2: resource acquire  methods                                        //
    //***************************************************************************************************************//
    boolean isFair();

    boolean tryAcquire(long timeoutNs);

    boolean tryRelease();

    boolean tryAcquireShared(long timeoutNs);

    boolean tryReleaseShared();

    boolean isHeldExclusively();

}
