/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beeop.pool;

import java.util.concurrent.Semaphore;

/**
 * Object Pool Semaphore
 *
 * @author Chris Liao
 * @version 1.0
 */
final class ObjectPoolSemaphore extends Semaphore {
    ObjectPoolSemaphore(int permits, boolean fair) {
        super(permits, fair);
    }

    void interruptWaitingThreads() {
        for (Thread thread : getQueuedThreads()) {
            Thread.State state = thread.getState();
            if (state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING) {
                thread.interrupt();
            }
        }
    }
}
