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

import org.stone.shine.synchronizer.extend.ResourceAction;

/**
 * Lock Base Action
 *
 * @author Chris Liao
 * @version 1.0
 */
abstract class LockAction extends ResourceAction {

    protected LockAtomicState lockState;

    LockAction(LockAtomicState lockState) {
        this.lockState = lockState;
    }

    LockAtomicState getLockState() {
        return lockState;
    }

    boolean isHeldByCurrentThread() {
        return lockState.isHeldByCurrentThread();
    }

//    Thread getExclusiveOwnerThread() {
//        return lockState.getExclusiveOwnerThread();
//    }
}