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
 * 阴阳锁特点
 * 1：锁值是一个64位数值，并分为高32位和低32位，高32位记录锁的类型和重入次数（针对读），低32位为阴阳核心转换区
 * 2：低32位值为偶数为阴锁（写锁，阴意味：阴冷的，下降的）；为奇数时则为阳锁（读锁，阳意味：活波的，热气的，上升的）
 * 3：锁转换，低32位值加1，锁发生转换（阳转阴，阴转阳）；加2则获得下一个同类锁（1+2=3,2+2=4），阴阳（奇偶）可以一直转换下去。
 *
 * @author Chris Liao
 * @version 1.0
 */
public class StampedLock implements java.io.Serializable {
    private static long READ_BASE = 1L << 56; //0100000000000000000000000000000000000000000000000000000000000000
    private static long WRITE_BASE = 3L << 56;//0110000000000000000000000000000000000000000000000000000000000000

    //read Lock(Locking):  00000001 00000000 00000000 00000001   00000000 00000000 00000000 00000000
    //read Lock(Released): 00000001 00000000 00000000 00000000   00000000 00000000 00000000 00000000
    //write Lock(Locking): 00000011 00000000 00000000 00000001   00000000 00000000 00000000 00000000
    //write Lock(Released):00000011 00000000 00000000 00000000   00000000 00000000 00000000 00000000
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
