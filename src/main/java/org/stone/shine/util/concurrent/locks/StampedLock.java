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

import org.stone.tools.CommonUtil;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * Stamped Lock Impl
 * <p>
 * StampedLock是stone并发包下的一个读写锁实现，它的内部有一个64位的Volatile型长整数，此数由高32位和低32位组成,
 * 分别表达不同含义，长整数的初始值为：2147483648L（可视为32高位皆为0，低32位为最小负整数）。
 * <p>
 * 1：高32位数值表达锁状态：是否持有，持有多少次。读锁持有时，高位值可大于等于1(读锁具有重入性和共享性），写锁持有时，
 * 高位值只能等于1；等于0时，表示锁已被释放或未被锁定，低位值与锁前一致（不变化）。高32位数的最高位值永久性为0，既重入
 * 的次数最高可达2147483647次（最大整数），也就是说数字戳，它永久性是一个long正数（stamp >=0）。
 * <p>
 * 2：低32位数为一个可递增的整数，它指征锁的类型，偶数时为写锁，奇数时为读锁，累加值，可更换锁的类型。加1则锁更换为
 * 下一个异类锁（奇转偶，偶转奇），加2则可获得下一个同类锁(奇转奇，偶转偶)。低位数递进过程可永无尽头，其奇偶替换规律，
 * 犹如阴阳替换，所以此锁又称之为：阴阳锁。（阴表示冷的，下降的，对应写锁；阳表示热的，上升的，对应读锁）
 * <p>
 * 3：抢锁成功后，32高位值位为1，32低位递进为奇数或偶数，上锁失败返回：-1
 * 4：当高位值大于0，低位数为偶数时(当前为写锁），线程抢锁失败。
 * 5：当高位值大于0，低位值为奇数时(当前为读锁)，且抢锁类型为读锁时，则累加高位数即可。
 * 6：当高位值等于0，低位值为偶数时(前次使用为写锁)，如果抢锁是读锁，高位设1， 低位值加1。
 * 7：当高位值等于0，低位值是奇数时(前次使用为读锁)，抢占锁是读锁时，高位设1，低位累加2。
 * 8：当高位值等于0，低位值为偶数时(前次使用为写锁），如果上抢占写锁，高位设1，低位加2。
 * 9：当高位值等于0，低位值为奇数时(前次使用为读锁），如果抢占写锁，高位设1，低位加1。
 * 10：写锁转读锁，低位加1，高位不变
 * 11：读锁转写锁，高位为0时，设高位为1，低位加1
 * <p>
 * 合并（高位|低位）的值，返回给用户。
 *
 * @author Chris Liao
 * @version 1.0
 */
public class StampedLock implements java.io.Serializable {
    private static final sun.misc.Unsafe UNSAFE;
    private static final long stampOffset;
    private static final long nodeStateOffset;
    //****************************************************************************************************************//
    //                                          1: static(5)                                                          //
    //****************************************************************************************************************//
    private static final int MOVE_SHIFT = 32;
    private static final long CLN_HIGH_MASK = 0xFFFFFFFFL;//4294967295L;
    private static final int WRITE_LOCK_FLAG = 0;
    private static final int READ_LOCK_FLAG = 1;

    static {
        try {
            UNSAFE = CommonUtil.UNSAFE;
            stampOffset = CommonUtil.objectFieldOffset(StampedLock.class, "stamp");
            nodeStateOffset = CommonUtil.objectFieldOffset(WaitNode.class, "state");
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private volatile long stamp = 2147483648L;
    private ConcurrentLinkedQueue<WaitNode> waitQueue = new ConcurrentLinkedQueue<>();//temporary

    //****************************************************************************************************************//
    //                                          2: Static(3)                                                          //
    //****************************************************************************************************************//
    private static int lowInt(long v) {
        return (int) (v & CLN_HIGH_MASK);
    }

    private static int highInt(long v) {
        return (int) (v >>> MOVE_SHIFT);
    }

    private static long contact(int h, int l) {
        return ((long) h << MOVE_SHIFT) | (l & CLN_HIGH_MASK);
    }

    private static boolean compareAndSetNodeState(WaitNode node, int exp, int upd) {
        return UNSAFE.compareAndSwapInt(node, nodeStateOffset, exp, upd);
    }

    private static boolean compareAndSetLockStamp(StampedLock lock, long exp, long upd) {
        return UNSAFE.compareAndSwapLong(lock, stampOffset, exp, upd);
    }

    private static boolean validate(long stamp1, long stamp2) {
        return stamp1 == stamp2 || (int) stamp1 == (int) stamp2;
    }

    private static long getReleaseStamp(long stamp) {
        int high = (int) (stamp >> 32);
        if (high > 0) {
            int low = (int) stamp;
            stamp = (long) (--high) << 32 | low & 0xFFFFFFFFL;
        }
        return stamp;
    }

    private static long getLockStamp(long stamp, boolean writeLock) {
        int low = (int) stamp;
        int high = (int) (stamp >> 32);
        boolean writeNumber = (low & 1) == 0;//low is an even number

        if (high == 0) {//in ununsing
            high = 1;
            low += writeLock == writeNumber ? 2 : 1;
        } else if (writeLock || writeNumber) {//write lock and write or read lock and write lock
            return -1;
        } else {//read lock(Reentrant)
            if (high + 1 <= 0) throw new Error("Maximum lock count exceeded");
            high++;
        }

        return (long) high << 32 | low & 0xFFFFFFFFL;
    }

    //****************************************************************************************************************//
    //                                          3: Read Lock                                                          //
    //****************************************************************************************************************//
    public long readLock() {
        return 1;
    }

    public long readLockInterruptibly() throws InterruptedException {
        return 1;
    }

    public long tryReadLock() {
        long newStamp = getLockStamp(this.stamp, false);
        if (newStamp > 0) compareAndSetLockStamp(this, stamp, newStamp);
        return newStamp;
    }

    public long tryReadLock(long time, TimeUnit unit) throws InterruptedException {
        return 1;
    }

    public void unlockRead(long stamp) {
        long currentStamp = this.stamp;
        if (!validate(stamp, currentStamp)) return;
        long newStamp = getReleaseStamp(currentStamp);
        if (newStamp != currentStamp && compareAndSetLockStamp(this, currentStamp, newStamp)) {
            int high = (int) (stamp >> 32);
            if (high == 0) {
                //wakeup other waiter
            }
        }
    }

    public boolean isReadLocked() {
        int low = (int) stamp;
        int high = (int) (stamp >> 32);
        return high > 0 && (low & 1) != 0;//Odd number == read
    }

    //****************************************************************************************************************//
    //                                          4: Write Lock                                                         //
    //****************************************************************************************************************//
    public void unlockWrite(long stamp) {
        long currentStamp = this.stamp;
        if (!validate(stamp, currentStamp)) return;
        long newStamp = getReleaseStamp(currentStamp);
        if (newStamp != currentStamp && compareAndSetLockStamp(this, currentStamp, newStamp)) {
            int high = (int) (stamp >> 32);
            if (high == 0) {
                //wakeup other waiter
            }
        }
    }

    public long writeLock() {
        return 1;
    }

    public long writeLockInterruptibly() throws InterruptedException {
        return 1;
    }

    public long tryWriteLock() {
        long newStamp = getLockStamp(this.stamp, true);
        if (newStamp > 0) compareAndSetLockStamp(this, stamp, newStamp);
        return newStamp;
    }

    public long tryWriteLock(long time, TimeUnit unit) throws InterruptedException {
        return 1;
    }

    public boolean isWriteLocked() {
        int low = (int) stamp;
        int high = (int) (stamp >> 32);
        return high > 0 && (low & 1) == 0;//even number  == write
    }

    //****************************************************************************************************************//
    //                                          5: Wait Node                                                          //
    //****************************************************************************************************************//
    private static class WaitNode {
        private final Thread thread;
        private final boolean isWrite;
        private volatile int state;//0:need signal,1:interrupted or timeout

        private WaitNode(boolean isWrite) {
            this.isWrite = isWrite;
            this.thread = Thread.currentThread();
        }

        public int getSate() {
            return state;
        }

        public Thread getThread() {
            return thread;
        }

        public boolean isWrite() {
            return isWrite;
        }

    }
}
