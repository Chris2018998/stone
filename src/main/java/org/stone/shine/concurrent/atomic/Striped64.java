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

import org.stone.util.CommonUtil;

import java.util.function.DoubleBinaryOperator;
import java.util.function.LongBinaryOperator;

import static java.lang.System.arraycopy;
import static org.stone.util.CommonUtil.NCPU;

/**
 * Cell Impl
 *
 * @author Chris Liao
 * @version 1.0
 */
abstract class Striped64 extends Number {

    //****************************************************************************************************************//
    //                                          1: base                                                               //
    //****************************************************************************************************************//
    private static final long CELLSBUSY;
    private static final long cellValueOffset;
    private static final sun.misc.Unsafe UNSAFE;
    private static final int RETRY_SIZE = NCPU;

    static {
        try {
            UNSAFE = CommonUtil.UNSAFE;
            cellValueOffset = CommonUtil.objectFieldOffset(Cell.class, "value");
            CELLSBUSY = CommonUtil.objectFieldOffset(Striped64.class, "cellsBusy");
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    transient Cell baseCell;
    transient volatile Cell[] cells;
    private transient volatile int cellsBusy;//cells lock

    //****************************************************************************************************************//
    //                                          2: constructor(by chris2018998）                                      //
    //****************************************************************************************************************//
    Striped64() {
        this.baseCell = new Cell(0);
    }

    Striped64(long baseV) {
        this.baseCell = new Cell(baseV);
    }

    Striped64(long baseV, int cellSize, long cellVal) {
        if (cellSize <= 0)
            throw new IllegalArgumentException("Cell size must be greater than zero");
        if (cellSize % 2 != 0)
            throw new IllegalArgumentException("Cell size is not a power of two");

        this.baseCell = new Cell(baseV);
        this.cells = new Cell[cellSize];
        for (int i = 0; i < cells.length; i++)
            cells[i] = new Cell(cellVal);
    }

    //****************************************************************************************************************//
    //                                          3: statics or cas (4)                                                 //
    //****************************************************************************************************************//
    private static int advanceProbe(int probe) {
        probe ^= probe << 13;
        probe ^= probe >>> 17;
        probe ^= probe << 5;
        return probe;
    }

    static boolean casCell(long x, Cell c, LongBinaryOperator fn) {
        final long v = c.value;
        return UNSAFE.compareAndSwapLong(c, cellValueOffset, v, fn.applyAsLong(v, x));
    }

    static boolean casCell(double x, Cell c, DoubleBinaryOperator fn) {
        final long v = c.value;
        final double v2 = Double.longBitsToDouble(v);
        return UNSAFE.compareAndSwapLong(c, cellValueOffset, v, Double.doubleToRawLongBits(fn.applyAsDouble(v2, x)));
    }

    private boolean casCellsBusy() {
        return UNSAFE.compareAndSwapInt(this, CELLSBUSY, 0, 1);
    }

    //****************************************************************************************************************//
    //                                          4: re-write method(by chris2018998）                                  //
    //****************************************************************************************************************//
    final void longAccumulate(long x, LongBinaryOperator fn) {
        int retrySize = RETRY_SIZE;
        int h = (int) Thread.currentThread().getId();//seed

        do {
            Cell[] as = cells;
            if (as != null) {
                //1: get a cell by hash
                h = advanceProbe(h);
                int n = as.length;
                int p = (n - 1) & h;
                Cell c = as[p];

                //2: create a new cell
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
                    } else {//lock failed
                        c = baseCell;
                    }
                }

                //3: cas cell
                if (casCell(x, c, fn)) return;

                //4: cells expand control
                if (n >= NCPU) continue;
                if (retrySize > 0) {
                    retrySize--;
                } else if (cellsBusy == 0 && casCellsBusy()) {
                    try {
                        as = cells;
                        if ((n = as.length) < NCPU) {
                            Cell[] rs = new Cell[n << 1];
                            arraycopy(as, 0, rs, 0, n);
                            rs[n] = new Cell(x);
                            cells = rs;
                            return;
                        }
                    } finally {
                        cellsBusy = 0;
                    }
                }
            } else if (cellsBusy == 0 && casCellsBusy()) {//cells is null
                try {
                    if (cells == null) {//recheck
                        cells = new Cell[]{null, new Cell(x)};
                        return;
                    }
                } finally {
                    cellsBusy = 0;
                }
            }
        } while (true);
    }

    //****************************************************************************************************************//
    //                                          5: re-write method(by chris2018998）                                  //
    //****************************************************************************************************************//
    final void doubleAccumulate(double x, DoubleBinaryOperator fn) {
        int retrySize = RETRY_SIZE;
        int h = (int) Thread.currentThread().getId();//seed

        do {
            Cell[] as = cells;
            if (as != null) {
                //1: get a cell by hash
                h = advanceProbe(h);
                int n = as.length;
                int p = (n - 1) & h;
                Cell c = as[p];

                //2: create a new cell
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
                    } else {//lock failed
                        c = baseCell;
                    }
                }

                //3: cas cell
                if (casCell(x, c, fn)) return;

                //4: cells expand control
                if (n >= NCPU) continue;
                if (retrySize > 0) {
                    retrySize--;
                } else if (cellsBusy == 0 && casCellsBusy()) {
                    try {
                        as = cells;
                        if ((n = as.length) < NCPU) {
                            Cell[] rs = new Cell[n << 1];
                            arraycopy(as, 0, rs, 0, n);
                            rs[n] = new Cell(Double.doubleToRawLongBits(x));
                            cells = rs;
                            return;
                        }
                    } finally {
                        cellsBusy = 0;
                    }
                }
            } else if (cellsBusy == 0 && casCellsBusy()) {//cells is null
                try {
                    if (cells == null) {//recheck
                        cells = new Cell[]{null, new Cell(Double.doubleToRawLongBits(x))};
                        return;
                    }
                } finally {
                    cellsBusy = 0;
                }
            }
        } while (true);
    }

    //****************************************************************************************************************//
    //                                          6: Cell Class                                                         //
    //****************************************************************************************************************//
    @sun.misc.Contended
    static final class Cell {
        volatile long value;

        private Cell(long x) {
            value = x;
        }
    }
}
