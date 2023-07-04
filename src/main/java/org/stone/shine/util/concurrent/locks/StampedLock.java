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
 * StampedLock是stone并发包下的一个读写锁实现，它的内部有一个64位的Volatile型长整形数，此数由高32位和低32位组成,
 * 分别表达不同含义，数的初始值为：2147483648L（可视为32高位皆为0，低32位为最小负整数转换的位数值）。
 * <p>
 * 1：高32位数值表达锁状态：是否持有，持有多少次。读锁持有时，高位值可大于等于1(读锁具有重入性和共享性），写锁持有时，
 * 高位值只能等于1；等于0时，表示锁已被释放，低位值与锁前一致（不变化）。高32位数的最高位值永久性为0，既重入的次数最高
 * 可达2147483647次（最大整数），也就是说数字戳，它永久性是一个long正数。
 * <p>
 * 2：低32位数为一个可递增的奇数或偶数，它指征锁的类型，偶数时为写锁，奇数时为读锁，累加值，可更换锁的类型。加1则锁更换为
 * 下一个异类锁（奇转偶，偶转奇），加2则可获得下一个同类锁(奇转奇，偶转偶)。低位数递进过程呈现奇偶替换现象，可永不停止递进，
 * 犹如阴阳替换，所以此锁又称之为：阴阳锁。（阴表示冷的，下降的，对应写锁；阳表示热的，上升的，对应读锁）
 * <p>
 * 3：抢锁成功后，32高位值位为1，32低位递进为奇数或偶数，上锁失败返回Long.MIN_VALUE。
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

    private static long decrementStamp(long stamp) {
        int high = (int) (stamp >> 32);
        if (high > 0) {
            int low = (int) stamp;
            stamp = (long) high << 32 | low & 0xFFFFFFFFL;
        }
        return stamp;
    }

    //Even number == write;Odd number == read
    private static long incrementStamp(long stamp, boolean writeLock) {
        int low = (int) stamp;
        int high = (int) (stamp >> 32);
        boolean writeNumber = (low & 1) == 0;//low is an even number

        if (writeLock) {//need write lock stamp
            if (high > 0) return 0;

            high = 1;
            low += writeNumber ? 2 : 1;
        } else if (writeNumber) {//stamp is write number
            if (high > 0) return 0;//lock in write

            high = 1;
            low++;
        } else {//
            if (high > 0) {
                high++;//increment reentrant count
            } else {
                high = 1;
                low++;
            }
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

//    public static void main(String[] ags) {
//        int SHARED_SHIFT = 16;
//        int EXCLUSIVE_MASK = 1 << SHARED_SHIFT - 1;
//        long half = 1L << 32;
//        long compare = half | Integer.MIN_VALUE;
//
//        String text1 = "0000000000000000000000000000000001111111111111111111111111111111";
//        String text2 = "0000000000000000000000000000000101111111111111111111111111111111";
//        long value = Long.parseLong(text2, 2);
//        long height = value >> 32;
//        long low = value << 32 >> 32;
//        System.out.println(height);
//        System.out.println(low);
//
//        long newValue = ++height << 32 | ++low;
//        System.out.println(convertBytes(Long.toBinaryString(newValue), 64, 1));
//    }

//    private static String convertBytes(String text, int totalSize, int splitSize) {
//        int len = totalSize - text.length();
//        if (len > 0) {
//            StringBuffer buf = new StringBuffer(len);
//            for (int i = 0; i < len; i++) buf.append("0");
//            text = buf.toString() + text;
//        }
//
//        System.out.println(text);
//        return text;
//        int startPos = 0, endPos;
//        StringBuffer splitBuf = new StringBuffer(totalSize);
//        while (startPos < totalSize) {
//            endPos = startPos + splitSize;
//            if (endPos > totalSize) endPos = totalSize;
//
//            if (startPos > 0) splitBuf.append(" ");
//            splitBuf.append(text, startPos, endPos);
//            startPos = endPos;
//        }
//        return splitBuf.toString();
//    }
}
