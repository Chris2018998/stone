/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.synchronizer.extend;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * resource state,which be similar to atomic field in {@link java.util.concurrent.locks.AbstractQueuedSynchronizer}class
 *
 * @author Chris Liao
 * @version 1.0
 */

public class ResourceAtomicState {

    private final AtomicInteger atomicState;

    public ResourceAtomicState() {
        this(0);
    }

    public ResourceAtomicState(int state) {
        this.atomicState = new AtomicInteger(state);
    }

    public final int getState() {
        return atomicState.get();
    }

    public final void setState(int newState) {
        atomicState.set(newState);
    }

    public final boolean compareAndSetState(int expect, int update) {
        return atomicState.compareAndSet(expect, update);
    }
}
