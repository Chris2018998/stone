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
