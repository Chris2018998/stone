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
    private static final int Exceeded = 2147483647;
    private volatile long stamp = 2147483648L;

    public static void main(String[] ags) {
        long stamp = 2147483648L;
        System.out.println(((int) stamp + 1));
    }

    private static boolean validateStamp(long stamp1, long stamp2) {
        return stamp1 == stamp2 || (int) stamp1 == (int) stamp2;
    }

    private static long releaseStamp(long stamp) {
        int high = (int) (stamp >> 32);
        if (high > 0) {
            int low = (int) stamp;
            stamp = (long) (--high) << 32 | low & 0xFFFFFFFFL;
        }
        return stamp;
    }

    private static long lockStamp(long stamp, boolean writeLock) {
        int low = (int) stamp;
        int high = (int) (stamp >> 32);
        boolean writeNumber = (low & 1) == 0;//low is an even number

        if (high == 0) {//in ununsing
            high = 1;
            low += writeLock == writeNumber ? 2 : 1;
        } else if (writeLock || writeNumber) {//write lock and write or read lock and write lock
            return -1;
        } else {//read lock(Reentrant)
            high++;
        }

        return (long) high << 32 | low & 0xFFFFFFFFL;
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
