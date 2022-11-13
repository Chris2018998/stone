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

    //****************************************************************************************************************//
    //                                          1: constructors (2)                                                   //
    //****************************************************************************************************************//
    public ReentrantLock() {
        this(false);
    }

    public ReentrantLock(boolean fair) {
        super(fair, new ReentrantLockAction(new ResourceAtomicState(0)));
    }

    //****************************************************************************************************************//
    //                                          2: monitor methods(4)                                                 //
    //****************************************************************************************************************//
    public boolean isLocked() {
        return lockAction.getAtomicStateValue() != 0;
    }

    public int getHoldCount() {
        return lockAction.getAtomicStateValue();
    }

    protected Thread getOwner() {
        return lockAction.getHoldThread();
    }

    public boolean isHeldByCurrentThread() {
        return lockAction.getHoldThread() == Thread.currentThread();
    }

    public String toString() {
        Thread o = lockAction.getHoldThread();
        return super.toString() + ((o == null) ?
                "[Unlocked]" :
                "[Locked by thread " + o.getName() + "]");
    }

    //Reentrant Action
    private static class ReentrantLockAction extends BaseLockAction {
        ReentrantLockAction(ResourceAtomicState lockState) {
            super(lockState);
        }

        public Object call(Object size) {
            if (this.getHoldThread() == Thread.currentThread()) {//reentrant
                int curState = this.getAtomicStateValue();
                curState++;
                if (curState <= 0) throw new Error("Maximum lock count exceeded");
                this.getLockState().setState(curState);
                return true;
            } else if (lockState.compareAndSetState(0, 1)) {
                this.setHoldThread(Thread.currentThread());
                return true;
            } else {
                return false;
            }
        }

        public boolean tryRelease(int size) {
            if (this.getHoldThread() == Thread.currentThread()) {
                int curState = this.getAtomicStateValue();
                if (curState > 0) {
                    this.getLockState().setState(curState - 1);
                    if (curState == 1) {
                        this.setHoldThread(null);
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }
}
