/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.concurrent.atomic;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.DoubleBinaryOperator;
import java.util.function.LongBinaryOperator;

/**
 * parts of this file are copy from JDK
 *
 * @author Chris Liao
 * @version 1.0
 */
abstract class Striped64 extends Number {

    //****************************************************************************************************************//
    //                                          1: Copy from JDK (by Doug Lea)                                        //
    //****************************************************************************************************************//
    private static final int NCPU = Runtime.getRuntime().availableProcessors();
    private static final sun.misc.Unsafe UNSAFE;
    private static final long BASE;
    private static final long CELLSBUSY;

    private static final int PROBE_INCREMENT = 0x9e3779b9;
    private static final AtomicInteger probeGenerator = new AtomicInteger();

    static {
        try {
            //UNSAFE = sun.misc.Unsafe.getUnsafe();
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe) theUnsafe.get(null);

            Class<?> sk = Striped64.class;
            BASE = UNSAFE.objectFieldOffset
                    (sk.getDeclaredField("base"));
            CELLSBUSY = UNSAFE.objectFieldOffset
                    (sk.getDeclaredField("cellsBusy"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    transient volatile Cell[] cells;
    transient volatile long base;
    transient volatile int cellsBusy;//cells lock

    private static int getProbe(int probe) {
        if (probe == 0) {
            int p = probeGenerator.addAndGet(PROBE_INCREMENT);
            return p == 0 ? 1 : p;
        } else {
            probe ^= probe << 13;
            probe ^= probe >>> 17;
            probe ^= probe << 5;
            return probe;
        }
    }

    final boolean casCellsBusy() {
        return UNSAFE.compareAndSwapInt(this, CELLSBUSY, 0, 1);
    }

    final boolean casBase(long cmp, long val) {
        return UNSAFE.compareAndSwapLong(this, BASE, cmp, val);
    }

    //****************************************************************************************************************//
    //                                          2: re-write method(by chris2018998）                                  //
    //****************************************************************************************************************//
    final void longAccumulate(long x, LongBinaryOperator fn) {

    }

    //****************************************************************************************************************//
    //                                          3: re-write method(by chris2018998）                                  //
    //****************************************************************************************************************//
    final void doubleAccumulate(double x, DoubleBinaryOperator fn) {

    }

    //****************************************************************************************************************//
    //                                          4: Cell(copy from JDK)                                                //
    //****************************************************************************************************************//
    @sun.misc.Contended
    static final class Cell {
        // Unsafe mechanics
        private static final sun.misc.Unsafe UNSAFE;
        private static final long valueOffset;

        static {
            try {
                Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                theUnsafe.setAccessible(true);
                UNSAFE = (Unsafe) theUnsafe.get(null);
                Class<?> ak = Cell.class;
                valueOffset = UNSAFE.objectFieldOffset
                        (ak.getDeclaredField("value"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }

        volatile long value;

        Cell(long x) {
            value = x;
        }

        final boolean cas(long cmp, long val) {
            return UNSAFE.compareAndSwapLong(this, valueOffset, cmp, val);
        }
    }
}
