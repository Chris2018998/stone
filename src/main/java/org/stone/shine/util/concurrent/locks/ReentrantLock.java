/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
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
            return lockState.exclusiveOwnerThread == Thread.currentThread() ? lockState.get() : 0;
        }

        public final Object call(Object size) {
            int state = lockState.get();
            if (state == 0) {
                if (lockState.compareAndSet(0, (int) size)) {
                    lockState.exclusiveOwnerThread = Thread.currentThread();
                    return Boolean.TRUE;
                }
            } else if (lockState.exclusiveOwnerThread == Thread.currentThread()) {//Reentrant
                state += (int) size;
                if (state <= 0) throw new Error("lock count increment exceeded");
                lockState.set(state);
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }

        public final boolean tryRelease(int size) {
            if (lockState.exclusiveOwnerThread == Thread.currentThread()) {
                int curState = lockState.get() - size;
                boolean free;
                if (free = (curState == 0))
                    lockState.exclusiveOwnerThread = null;

                lockState.set(curState);
                return free;
            } else {
                throw new IllegalMonitorStateException();
            }
        }
    }
}