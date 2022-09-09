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
 * for Lock(radLock/)
 */
public interface SharedPermitPool extends ReusePermitPool {

    boolean isInSharingState();

    boolean isHeldExclusively();

    boolean tryReleaseShared();

    boolean acquireSharedUninterruptibly(long timeoutNs);

    boolean acquireShared(long timeoutNs) throws InterruptedException;

}
