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

public abstract class ResourceStateAction extends ResourceAction {

    private AtomicInteger state = new AtomicInteger();

    protected final int getState() {
        return state.get();
    }

    protected final void setState(int newState) {
        state.set(newState);
    }

    protected final boolean compareAndSetState(int expect, int update) {
        return state.compareAndSet(expect, update);
    }
}
