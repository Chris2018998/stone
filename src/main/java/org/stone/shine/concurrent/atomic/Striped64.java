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
    //                                          1: Copy from JDK                                                      //
    //****************************************************************************************************************//
    private static final long BASE;
    private static final long CELLSBUSY;
    private static final sun.misc.Unsafe UNSAFE;
    private static final int NCPU = Runtime.getRuntime().availableProcessors();
    private static final int RETRY_SIZE = 16;

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

    transient volatile long base;
    transient volatile Cell[] cells;
    private transient volatile int cellsBusy;//cells lock

    private static int advanceProbe(int probe) {
        probe ^= probe << 13;
        probe ^= probe >>> 17;
        probe ^= probe << 5;
        return probe;
    }

    boolean casBase(long cmp, long val) {
        return UNSAFE.compareAndSwapLong(this, BASE, cmp, val);
    }

    private boolean casCellsBusy() {
        return UNSAFE.compareAndSwapInt(this, CELLSBUSY, 0, 1);
    }

    //****************************************************************************************************************//
    //                                          2: re-write method(by chris2018998）                                  //
    //****************************************************************************************************************//
    final void longAccumulate(long x, LongBinaryOperator fn) {
        int retrySize = RETRY_SIZE;
        int h = (int) Thread.currentThread().getId();

        do {
            long v = base;
            if (casBase(v, fn.applyAsLong(v, x))) return;

            Cell[] as = cells;
            if (as != null) {
                h = advanceProbe(h);
                final int n = as.length;
                final int p = n - 1 & h;
                Cell c = as[p];
                if (c == null) {
                    if (cellsBusy == 0 && casCellsBusy()) {
                        try {
                            Cell[] rs = cells;
                            if ((c = rs[p]) == null) {
                                rs[p] = new Cell(x);
                                return;
                            }
                        } finally {
                            cellsBusy = 0;
                        }

                        v = c.value;
                        if (c.cas(v, fn.applyAsLong(v, x))) return;
                    }
                } else {
                    v = c.value;
                    if (c.cas(v, fn.applyAsLong(v, x))) return;
                }

                //cells expand control
                if (n >= NCPU) continue;
                if (retrySize > 0) {
                    retrySize--;
                } else if (as == cells && cellsBusy == 0 && casCellsBusy()) {
                    try {
                        Cell[] rs = new Cell[n << 1];
                        System.arraycopy(as, 0, rs, 0, n);
                        rs[n] = new Cell(x);
                        cells = rs;
                        return;
                    } finally {
                        cellsBusy = 0;
                    }
                }
            } else if (cellsBusy == 0 && casCellsBusy()) {//cells is null
                try {
                    if (cells == null) {
                        Cell[] rs = new Cell[2];
                        rs[0] = new Cell(x);
                        cells = rs;
                        return;
                    }
                } finally {
                    cellsBusy = 0;
                }
            }
        } while (true);
    }

    //****************************************************************************************************************//
    //                                          3: re-write method(by chris2018998）                                  //
    //****************************************************************************************************************//
    final void doubleAccumulate(double x, DoubleBinaryOperator fn) {
        int retrySize = RETRY_SIZE;
        int h = (int) Thread.currentThread().getId();

        do {
            long v = base;
            if (casBase(v, Double.doubleToRawLongBits(fn.applyAsDouble(Double.longBitsToDouble(v), x)))) return;

            Cell[] as = cells;
            if (as != null) {
                h = advanceProbe(h);
                final int n = as.length;
                final int p = n - 1 & h;
                Cell c = as[p];
                if (c == null) {
                    if (cellsBusy == 0 && casCellsBusy()) {
                        try {
                            Cell[] rs = cells;
                            if ((c = rs[p]) == null) {
                                rs[p] = new Cell(Double.doubleToRawLongBits(x));
                                return;
                            }
                        } finally {
                            cellsBusy = 0;
                        }

                        v = c.value;
                        if (c.cas(v, Double.doubleToRawLongBits(fn.applyAsDouble(Double.longBitsToDouble(v), x))))
                            return;
                    }
                } else {
                    v = c.value;
                    if (c.cas(v, Double.doubleToRawLongBits(fn.applyAsDouble(Double.longBitsToDouble(v), x))))
                        return;
                }

                //cells expand control
                if (n >= NCPU) continue;
                if (retrySize > 0) {
                    retrySize--;
                } else if (as == cells && cellsBusy == 0 && casCellsBusy()) {
                    try {
                        Cell[] rs = new Cell[n << 1];
                        System.arraycopy(as, 0, rs, 0, n);
                        rs[n] = new Cell(Double.doubleToRawLongBits(x));
                        cells = rs;
                        return;
                    } finally {
                        cellsBusy = 0;
                    }
                }
            } else if (cellsBusy == 0 && casCellsBusy()) {//cells is null
                try {
                    if (cells == null) {
                        Cell[] rs = new Cell[2];
                        rs[0] = new Cell(Double.doubleToRawLongBits(x));
                        cells = rs;
                        return;
                    }
                } finally {
                    cellsBusy = 0;
                }
            }
        } while (true);
    }

    //****************************************************************************************************************//
    //                                          4: AtomicCell(copy from JDK)                                                //
    //****************************************************************************************************************//
    @sun.misc.Contended
    static final class Cell {
        private static final long valueOffset;

        static {
            try {
                Class<?> ak = Cell.class;
                valueOffset = UNSAFE.objectFieldOffset
                        (ak.getDeclaredField("value"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }

        volatile long value;

        private Cell(long x) {
            value = x;
        }

        final boolean cas(long cmp, long val) {
            return UNSAFE.compareAndSwapLong(this, valueOffset, cmp, val);
        }
    }
}
