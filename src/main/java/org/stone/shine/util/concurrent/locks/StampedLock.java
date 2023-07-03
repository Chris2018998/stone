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
 * 3：锁转换，低32位值加1，锁发生转换（阳转阴，阴转阳）；加2则获得下一个同类锁（1+2=3,2+2=4），阴阳（奇偶）可以一直前进转换下去。
 * 4: 任意设定一个初始Int型整数（负数也可），放入低32位中，高位则标记锁的初始类型和未用状态
 * 5：抢锁时，先读取锁的类型，锁的状态，决定将低32位部分加1还是加2,再CAS放入值（如果抢占读锁，且当前锁正式读锁和重入部分值大于0，则累计重入部分)
 *
 * @author Chris Liao
 * @version 1.0
 */
public class StampedLock implements java.io.Serializable {
    private static long READ_BASE = 1L << 56; //0000000100000000000000000000000000000000000000000000000000000000
    private static long WRITE_BASE = 3L << 56;//0000001100000000000000000000000000000000000000000000000000000000

    //read Lock(Locking):  00000001 00000000 00000000 00000001  00000000 00000000 00000000 00000000
    //read Lock(Released): 00000001 00000000 00000000 00000000  00000000 00000000 00000000 00000000
    //write Lock(Locking): 00000011 00000000 00000000 00000001  00000000 00000000 00000000 00000000
    //write Lock(Released):00000011 00000000 00000000 00000000  00000000 00000000 00000000 00000000
    private volatile long currentStamp;

    public static void main(String[] ags) {
        //System.out.println(SHARED_UNIT);
        //System.out.println(MAX_COUNT);
        // System.out.println(EXCLUSIVE_MASK);
        String intText = "01111111111111111111111111111111";
        System.out.println(Integer.parseInt(intText, 2));
        System.out.println(Integer.MAX_VALUE % 2 == 0);
        System.out.println((Integer.MAX_VALUE + 1) % 2 == 0);


        String text = "0000000100000000000000000000000000000000000000000000000000000000";
        String text2 = "0000001100000000000000000000000000000000000000000000000000000000";
        long value = Long.parseLong(text, 2);
        long value2 = Long.parseLong(text2, 2);
        int size = 56;
        long read1 = 1L << size;
        long write1 = 3L << size;
        if (text.equals(convertBytes(Long.toBinaryString(read1), 64, 8))) {
            System.out.println("read success");
        }
        if (text2.equals(convertBytes(Long.toBinaryString(write1), 64, 8))) {
            System.out.println("write success");
        }
    }

    private static void testLongType(long stamp) {
        if ((stamp | READ_BASE) == READ_BASE) {
            System.out.println("Read");
        } else if ((stamp | WRITE_BASE) == WRITE_BASE) {
            System.out.println("Write");
        }
    }


    private static String convertBytes(String text, int totalSize, int splitSize) {
        int len = totalSize - text.length();
        if (len > 0) {
            StringBuffer buf = new StringBuffer(len);
            for (int i = 0; i < len; i++) buf.append("0");
            text = buf.toString() + text;
        }

        System.out.println(text);
        return text;
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
    }


    //****************************************************************************************************************//
    //                                          1: cal next stamp                                                     //
    //****************************************************************************************************************//
    private long getNextReadStamp() {

        return currentStamp;//@todo
    }

    private long getNextWriteStamp() {
        return currentStamp;//@todo
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
