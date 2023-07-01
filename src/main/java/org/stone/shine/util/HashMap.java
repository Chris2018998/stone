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

import java.util.concurrent.TimeUnit;

/**
 * Stamped Lock Impl
 *
 * @author Chris Liao
 * @version 1.0
 */
public class StampedLock implements java.io.Serializable {

    //read Lock(Locking):  01000000 00000000 00000000 00000001   00000000 00000000 00000000 00000000
    //read Lock(Released): 01000000 00000000 00000000 00000000   00000000 00000000 00000000 00000000
    //write Lock(Locking): 01100000 00000000 00000000 00000001   00000000 00000000 00000000 00000000
    //write Lock(Released):01100000 00000000 00000000 00000000   00000000 00000000 00000000 00000000
    private volatile long lockState;


    //****************************************************************************************************************//
    //                                          1: cal next stamp                                                     //
    //****************************************************************************************************************//
    private long getNextReadStamp() {
        return lockState;//@todo
    }

    private long getNextWriteStamp() {
        return lockState;//@todo
    }

    //****************************************************************************************************************//
    //                                          1: Read Lock                                                          //
    //****************************************************************************************************************//
    public void unlockRead(long stamp) {

    }

    public boolean isReadLocked() {
        return true;
    }

    public long readLock() {
        return 1;
    }

    public long readLockInterruptibly() throws InterruptedException {
        return 1;
    }

    public long tryReadLock() {
        return 1;
    }

    public long tryReadLock(long time, TimeUnit unit) throws InterruptedException {
        return 1;
    }

    //****************************************************************************************************************//
    //                                          2: Write Lock                                                         //
    //****************************************************************************************************************//
    public void unlockWrite(long stamp) {
    }

    public boolean isWriteLocked() {
        return true;
    }

    public long writeLock() {
        return 1;
    }

    public long writeLockInterruptibly() throws InterruptedException {
        return 1;
    }

    public long tryWriteLock() {
        return 1;
    }

    public long tryWriteLock(long time, TimeUnit unit) throws InterruptedException {
        return 1;
    }
}
