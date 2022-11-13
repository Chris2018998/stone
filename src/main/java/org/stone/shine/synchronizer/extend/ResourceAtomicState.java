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
 * @author Chris Liao
 * @version 1.0
 */

public final class ResourceAtomicState {

    private final AtomicInteger atomicState = new AtomicInteger();

    public ResourceAtomicState(int state) {
        atomicState.set(state);
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
