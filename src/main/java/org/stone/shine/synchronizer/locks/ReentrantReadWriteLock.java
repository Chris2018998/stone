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

import org.stone.shine.synchronizer.extend.AcquireTypes;
import org.stone.shine.synchronizer.extend.ResourceAtomicState;
import org.stone.shine.synchronizer.extend.ResourceWaitPool;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * ReadWrite Lock Implementation
 *
 * @author Chris Liao
 * @version 1.0
 */
public class ReentrantReadWriteLock implements ReadWriteLock {
    //resource wait Pool(share to WriteLockImpl,ReadLockImpl)
    private final ResourceWaitPool waitPool;
    //lockState(share to WriteLockImpl,ReadLockImpl)
    private final ResourceAtomicState lockState;
    //Inner class providing readLock
    private final ReentrantReadWriteLock.ReadLock readerLock;
    //Inner class providing writeLock
    private final ReentrantReadWriteLock.WriteLock writerLock;

    //****************************************************************************************************************//
    //                                          1: constructors (2)                                                   //
    //****************************************************************************************************************//
    public ReentrantReadWriteLock() {
        this(false);
    }

    public ReentrantReadWriteLock(boolean fair) {
        this.waitPool = new ResourceWaitPool(fair);
        this.lockState = new ResourceAtomicState(0);
        this.writerLock = new WriteLock(waitPool, new WriteLockAction(lockState));
        this.readerLock = new ReadLock(waitPool, new ReadLockAction(lockState));
    }

    /**
     * Returns the lock used for reading.
     *
     * @return the lock used for reading
     */
    public Lock readLock() {
        return readerLock;
    }

    /**
     * Returns the lock used for writing.
     *
     * @return the lock used for writing
     */
    public Lock writeLock() {
        return writerLock;
    }

    //****************************************************************************************************************//
    //                                       2: WriteLock/ReadLock Impl                                               //                                                                                  //
    //****************************************************************************************************************//
    private static class WriteLock extends AbstractLock {
        WriteLock(ResourceWaitPool waitPool, BaseLockAction lockAction) {
            super(waitPool, lockAction, AcquireTypes.TYPE_Exclusive);
        }
    }

    private static class ReadLock extends AbstractLock {
        ReadLock(ResourceWaitPool waitPool, BaseLockAction lockAction) {
            super(waitPool, lockAction, AcquireTypes.TYPE_Exclusive);
        }

        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }

    //****************************************************************************************************************//
    //                                       3: Lock Action Impl                                                      //
    //****************************************************************************************************************//
    private static class WriteLockAction extends BaseLockAction {
        WriteLockAction(ResourceAtomicState lockState) {
            super(lockState);
        }

        public Object call(Object size) {
            //@todo
            return true;
        }

        public boolean tryRelease(int size) {
            //@todo
            return true;
        }
    }

    private static class ReadLockAction extends BaseLockAction {
        ReadLockAction(ResourceAtomicState lockState) {
            super(lockState);
        }

        public Object call(Object size) {
            //@todo
            return true;
        }

        public boolean tryRelease(int size) {
            //@todo
            return true;
        }
    }
}
