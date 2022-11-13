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
    //lock state
    private ResourceAtomicState lockState;
    //lock state
    private ResourceAtomicState lockState;


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
        return holdCount;
    }

    public boolean isLocked() {
        return ownerRef.get() != null;
    }

    public boolean isHeldByCurrentThread() {
        return ownerRef.get() == Thread.currentThread();
    }

    protected Thread getOwner() {
        return ownerRef.get();
    }


    public String toString() {
        Thread o = ownerRef.get();
        return super.toString() + ((o == null) ?
                "[Unlocked]" :
                "[Locked by thread " + o.getName() + "]");
    }
}
