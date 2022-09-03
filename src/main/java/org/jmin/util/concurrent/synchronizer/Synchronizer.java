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

public interface Synchronizer extends SynchronizerWaitChain {

    //******************************************resource state/size method  *********************************************/
    int getState();

    void setState(int newState);

    boolean compareAndSetState(int expect, int update);

    //******************************************resource acquire method  *********************************************/
    boolean isFair();

    boolean tryAcquire(long waitNanos);

    boolean tryRelease();

    boolean tryAcquireShared(long waitNanos);

    boolean tryReleaseShared();

    boolean isHeldExclusively();

}
