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

/**
 * Reentrant Lock Implementation
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class ReentrantLock extends BaseLock {

    public ReentrantLock() {
        this(false);
    }

    public ReentrantLock(boolean fair) {
        super(fair, new ReentrantLockAction(new LockAtomicState()));
    }

    public String toString() {
        Thread o = this.getOwner();
        return super.toString() + ((o == null) ?
                "[Unlocked]" :
                "[Locked by thread " + o.getName() + "]");
    }

    //Reentrant Action
    private static class ReentrantLockAction extends LockAction {
        ReentrantLockAction(LockAtomicState lockState) {
            super(lockState);
        }

        public final int getHoldCount() {
            return lockState.getState();
        }

        public final Object call(Object size) {
            int state = lockState.getState();
            if (state == 0) {
                if (lockState.compareAndSetState(0, (int) size)) {
                    lockState.setExclusiveOwnerThread(Thread.currentThread());
                    return true;
                }
                return false;
            } else if (lockState.getExclusiveOwnerThread() == Thread.currentThread()) {//Reentrant
                state += (int) size;
                if (state <= 0) throw new Error("lock count increment exceeded");
                lockState.setState(state);
                return true;
            }
            return false;
        }

        public final boolean tryRelease(int size) {
            if (lockState.getExclusiveOwnerThread() == Thread.currentThread()) {
                int curState = lockState.getState() - size;//full release(occur in condition wait)

                if (curState < 0) throw new Error("lock count decrement exceeded");
                if (curState == 0) lockState.setExclusiveOwnerThread(null);
                lockState.setState(curState);
                return curState == 0;
            } else {
                return false;
            }
        }
    }
}
