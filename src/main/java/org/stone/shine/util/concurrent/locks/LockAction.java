/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.util.concurrent.locks;

import org.stone.shine.util.concurrent.synchronizer.extend.ResourceAction;

/**
 * Lock Base Action
 *
 * @author Chris Liao
 * @version 1.0
 */
abstract class LockAction extends ResourceAction {

    protected final LockAtomicState lockState;

    LockAction(LockAtomicState lockState) {
        this.lockState = lockState;
    }

    abstract int getHoldCount();

    LockAtomicState getLockState() {
        return lockState;
    }

    boolean isHeldByCurrentThread() {
        return lockState.isHeldByCurrentThread();
    }
}
