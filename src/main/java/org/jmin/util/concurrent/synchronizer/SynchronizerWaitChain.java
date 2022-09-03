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

public interface SynchronizerWaitChain {

    void addWaiter(int stateCode);

    void addWaiter(int stateCode, long waitNanos);

    void signal(int stateCode);

    void signalAll(int stateCode);

    int getQueueLength(int stateCode);

    boolean hasQueuedThreads(int stateCode);
}




