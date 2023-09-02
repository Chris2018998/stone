/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.util.concurrent.synchronizer.resource;

import org.stone.tools.atomic.UnsafeAdaptor;
import org.stone.tools.atomic.UnsafeAdaptorHolder;

/**
 * resource state,which be similar to atomic field in {@link java.util.concurrent.locks.AbstractQueuedSynchronizer}class
 *
 * @author Chris Liao
 * @version 1.0
 */

public class AtomicLongState {
    private final static long offset;
    private final static UnsafeAdaptor U;

    static {
        try {
            U = UnsafeAdaptorHolder.UA;
            offset = U.objectFieldOffset(AtomicIntState.class.getDeclaredField("state"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private volatile long state;

    public AtomicLongState() {
    }

    public AtomicLongState(long state) {
        this.state = state;
    }

    public final long getState() {
        return state;
    }

    public final void setState(long newState) {
        U.putLongVolatile(this, offset, newState);
    }

    public final boolean compareAndSetState(long expect, long update) {
        return U.compareAndSwapLong(this, offset, expect, update);
    }
}
