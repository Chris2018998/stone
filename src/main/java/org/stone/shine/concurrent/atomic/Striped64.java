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
    //                                          1: Copy from JDK                                                      //
    //****************************************************************************************************************//
    private static final long BASE;
    private static final long CELLSBUSY;
    private static final sun.misc.Unsafe UNSAFE;
    private static final int NCPU = Runtime.getRuntime().availableProcessors();
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

    transient volatile long base;
    transient volatile Cell[] cells;
    private transient volatile int cellsBusy;//cells lock

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

    private static Cell[] createCells(Cell[] oldCells, int newLen, long x, int index) {
        Cell[] newCells = new Cell[newLen];
        if (oldCells != null) {
            int oldLen = oldCells.length;
            System.arraycopy(oldCells, oldLen, newCells, 0, oldLen);
        }
        newCells[index] = new Cell(x);
        return newCells;
    }

    private boolean casBase(long cmp, long val) {
        return UNSAFE.compareAndSwapLong(this, BASE, cmp, val);
    }

    private boolean casCellsBusy() {
        return UNSAFE.compareAndSwapInt(this, CELLSBUSY, 0, 1);
    }

    //****************************************************************************************************************//
    //                                          2: re-write method(by chris2018998）                                  //
    //****************************************************************************************************************//
    final void longAccumulate(long x, LongBinaryOperator fn) {
        int probe = 0;
        int retrySize = 16;

        do {
            //1: try to add to base
            long currentV = base;
            if (casBase(currentV, fn != null ? fn.applyAsLong(currentV, x) : currentV + x)) return;

            //2:if cell is not null
            Cell[] array = cells;
            if (array != null) {
                int index = (array.length - 1) & getProbe(probe);
                Cell cell = array[index];
                if (cell != null) {
                    long cellV = cell.value;
                    if (cell.cas(cellV, fn != null ? fn.applyAsLong(cellV, x) : cellV + x)) return;
                } else if (casCellsBusy()) {//need fill a new cell
                    try {
                        Cell[] array2 = cells;
                        if (array == array2 && array2[index] == null) {
                            array2[index] = new Cell(x);
                            return;
                        }
                    } finally {
                        cellsBusy = 0;
                    }
                }

                //when expand cells?
                if (retrySize > 0) {
                    retrySize--;
                } else {
                    //@todo expand cells
                    retrySize = 16;
                }
            } else if (casCellsBusy()) {//create initial cells array
                try {
                    this.cells = createCells(null, 2, x, 0);
                    return;
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

    }

    //****************************************************************************************************************//
    //                                          4: Cell(copy from JDK)                                                //
    //****************************************************************************************************************//
    @sun.misc.Contended
    static final class Cell {
        private static final long valueOffset;
        private static final sun.misc.Unsafe UNSAFE;

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

        private Cell(long x) {
            value = x;
        }

        final boolean cas(long cmp, long val) {
            return UNSAFE.compareAndSwapLong(this, valueOffset, cmp, val);
        }
    }
}
