/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.synchronizer.locks;

import org.stone.shine.synchronizer.extend.ResourceAtomicState;

/**
 * Reentrant Lock Implementation
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class ReentrantLock extends AbstractLock {
    //
    private final ResourceAtomicState lockState = new ResourceAtomicState(0);

    //****************************************************************************************************************//
    //                                          1: constructors(2)                                                    //
    //****************************************************************************************************************//
    public ReentrantLock() {
        this(false);
    }

    public ReentrantLock(boolean fair) {
        super(fair, null);
    }

    //****************************************************************************************************************//
    //                                          3: monitor methods                                                    //
    //****************************************************************************************************************//
    public int getHoldCount() {
        return lockState.getState();
    }

    public boolean isLocked() {
        return lockState.getState() != 0;
    }

    protected Thread getOwner() {
        return ownerRef.get();
    }

    public Thread getHoldThread() {
        return null;
    }

    public boolean isHeldByCurrentThread() {
        return ownerRef.get() == Thread.currentThread();
    }

    public String toString() {
        Thread o = ownerRef.get();
        return super.toString() + ((o == null) ?
                "[Unlocked]" :
                "[Locked by thread " + o.getName() + "]");
    }
}
