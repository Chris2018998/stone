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
 * <p>
 * max size =1
 * <p>
 * design for Lock
 */
public interface SharePermitPool extends ReusePermitPool {

    boolean isInSharingState();

    boolean isHeldExclusively();

    boolean releaseShared();

    boolean acquireSharedUninterruptibly(long deadlineNs);

    boolean acquireShared(long deadlineNs) throws InterruptedException;

    void afterAcquired(SynchronizeNode node);
}
