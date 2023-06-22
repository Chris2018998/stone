/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.util.concurrent.synchronizer.locks;

import org.stone.shine.util.concurrent.synchronizer.extend.AtomicIntState;

/**
 * Lock Atomic state
 *
 * @author Chris Liao
 * @version 1.0
 */
final class LockAtomicState extends AtomicIntState {

    private Thread exclusiveOwnerThread;

    Thread getExclusiveOwnerThread() {
        return exclusiveOwnerThread;
    }

    void setExclusiveOwnerThread(Thread exclusiveOwnerThread) {
        this.exclusiveOwnerThread = exclusiveOwnerThread;
    }

    boolean isHeldByCurrentThread() {
        return exclusiveOwnerThread == Thread.currentThread();
    }
}
