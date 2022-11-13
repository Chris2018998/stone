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
import org.stone.shine.synchronizer.extend.ResourceAtomicState;

/**
 * Lock Base Action
 *
 * @author Chris Liao
 * @version 1.0
 */
abstract class BaseLockAction extends ResourceAction {

    private Thread holdThread;

    private ResourceAtomicState lockState;

    BaseLockAction(ResourceAtomicState lockState) {
        this.lockState = lockState;
    }

    public int getAtomicStateValue() {
        return lockState.getState();
    }

    public ResourceAtomicState getLockState() {
        return lockState;
    }

    public Thread getHoldThread() {
        return holdThread;
    }

    public void setHoldThread(Thread holdThread) {
        this.holdThread = holdThread;
    }
}
