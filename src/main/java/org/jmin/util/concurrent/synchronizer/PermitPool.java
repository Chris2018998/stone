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
public interface PermitPool extends SynchronizeNodeChain {

    boolean isFair();

    boolean release(int args);

    boolean acquireUninterruptibly(long deadlineNs);

    boolean acquire(long deadlineNs) throws InterruptedException;

}
