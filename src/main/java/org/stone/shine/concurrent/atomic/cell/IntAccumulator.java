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
 * int Accumulator
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class IntAccumulator extends IntCellBase implements Serializable {
    private final int identity;

    public IntAccumulator(IntBinaryOperator function, int identity) {
        if (function == null) throw new NullPointerException();
        this.intFunction = function;
        this.base = identity;
        this.identity = identity;
    }

    public void accumulate(int x) {
        int v = base;
        if (!casBase(v, intFunction.applyAsInt(v, x)))
            longAccumulate(x);
    }

    public int get() {
        Cell[] as = cells;
        Cell a;

        int result = base;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null)
                    result = intFunction.applyAsInt(result, a.value);
            }
        }
        return result;
    }

    public void reset() {
        Cell[] as = cells;
        Cell a;
        base = identity;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null)
                    a.value = identity;
            }
        }
    }

    public int getThenReset() {
        Cell[] as = cells;
        Cell a;
        int result = base;
        base = identity;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null) {
                    int v = a.value;
                    a.value = identity;
                    result = intFunction.applyAsInt(result, v);
                }
            }
        }
        return result;
    }

    public double doubleValue() {
        return get();
    }

    public long longValue() {
        return (long) get();
    }

    public int intValue() {
        return get();
    }

    public float floatValue() {
        return (float) get();
    }

    public String toString() {
        return Double.toString(get());
    }
}
