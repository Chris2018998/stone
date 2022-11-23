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

/**
 * Reentrant Lock Implementation
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class ReentrantLock extends BaseLock {
    //****************************************************************************************************************//
    //                                          1: constructors (2)                                                   //
    //****************************************************************************************************************//
    public ReentrantLock() {
        this(false);
    }

    public ReentrantLock(boolean fair) {
        super(fair, new ReentrantLockAction(new LockAtomicState()));
    }

    //****************************************************************************************************************//
    //                                          2: monitor methods(4)                                                 //
    //****************************************************************************************************************//
    public boolean isLocked() {
        return lockState.getState() != 0;
    }

    public int getHoldCount() {
        return lockState.getState();
    }

    protected Thread getOwner() {
        return lockState.getExclusiveOwnerThread();
    }

    public boolean isHeldByCurrentThread() {
        return lockState.isHeldByCurrentThread();
    }

    public String toString() {
        Thread o = lockState.getExclusiveOwnerThread();
        return super.toString() + ((o == null) ?
                "[Unlocked]" :
                "[Locked by thread " + o.getName() + "]");
    }

    //Reentrant Action
    private static class ReentrantLockAction extends LockAction {
        ReentrantLockAction(LockAtomicState lockState) {
            super(lockState);
        }

        public Object call(Object size) {
            int state = lockState.getState();
            if (state == 0) {
                if (lockState.compareAndSetState(0, 1)) {
                    lockState.setExclusiveOwnerThread(Thread.currentThread());
                    return true;
                } else {
                    return false;
                }
            } else if (lockState.getExclusiveOwnerThread() == Thread.currentThread()) {//Reentrant
                state++;
                if (state <= 0) throw new Error("Maximum lock count exceeded");
                lockState.setState(state);
                return true;
            } else {
                return false;
            }
        }

        public boolean tryRelease(int size) {
            if (lockState.getExclusiveOwnerThread() == Thread.currentThread()) {
                int curState = lockState.getState();
                if (curState == 1) lockState.setExclusiveOwnerThread(null);
                if (curState > 0) lockState.setState(curState - 1);
                return curState == 1;
            } else {
                return false;
            }
        }
    }
}
