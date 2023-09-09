/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.util.concurrent.synchronizer.extend;

/**
 * resource state,which be similar to atomic field in {@link java.util.concurrent.locks.AbstractQueuedSynchronizer}class
 *
 * @author Chris Liao
 * @version 1.0
 */

public class AtomicIntState {
    private final static long offset;
    private final static sun.misc.Unsafe U;

    static {
        try {
            U = org.stone.tools.atomic.UnsafeAdaptorSunMiscImpl.U;
            offset = U.objectFieldOffset(AtomicIntState.class.getDeclaredField("state"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private volatile int state;

    public AtomicIntState() {
    }

    public AtomicIntState(int state) {
        this.state = state;
    }

    public final int getState() {
        return state;
    }

    public final void setState(int newState) {
        U.putIntVolatile(this, offset, newState);
    }

    public final boolean compareAndSetState(int expect, int update) {
        return U.compareAndSwapInt(this, offset, expect, update);
    }
}
