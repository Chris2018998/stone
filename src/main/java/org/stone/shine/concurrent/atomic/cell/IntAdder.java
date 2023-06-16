/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.shine.concurrent.atomic.cell;

import java.io.Serializable;
import java.util.function.IntBinaryOperator;

/**
 * int cell base object
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class IntAdder extends IntCellBase implements Serializable {
    private final static IntBinaryOperator operator = new IntAddrOperator();
    private final int initial;

    public IntAdder() {
        this.initial = 0;
        this.intFunction = operator;
    }

    public IntAdder(int initial) {
        this.initial = initial;
        this.base = initial;
        this.intFunction = operator;
    }

    public void add(int x) {
        int v = base;
        if (!casBase(v, v + x)) longAccumulate(x);
    }

    public void increment() {
        add(1);
    }

    public void decrement() {
        add(-1);
    }

    public long sum() {
        Cell[] as = cells;
        Cell a;
        int sum = base;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null)
                    sum += a.value;
            }
        }
        return sum;
    }

    public void reset() {
        Cell[] as = cells;
        Cell a;
        base = initial;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null)
                    a.value = 0;
            }
        }
    }

    public int sumThenReset() {
        Cell[] as = cells;
        Cell a;
        int sum = base;
        base = initial;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null) {
                    sum += a.value;
                    a.value = 0;
                }
            }
        }
        return sum;
    }

    public long longValue() {
        return sum();
    }

    public int intValue() {
        return (int) sum();
    }

    public float floatValue() {
        return (float) sum();
    }

    public double doubleValue() {
        return (double) sum();
    }

    public String toString() {
        return Long.toString(sum());
    }

    private static class IntAddrOperator implements IntBinaryOperator {
        public final int applyAsInt(int left, int right) {
            return left + right;
        }
    }
}