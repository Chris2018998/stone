/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.tools.atomic;

import sun.misc.Unsafe;

/**
 * fixed Length object array
 *
 * @author Chris Liao
 * @version 1.0
 */
public class AtomicRingArray<T> {
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final int shift = 0;
    private static final int base = unsafe.arrayBaseOffset(Object[].class);

    private final int capacity;
    private final Object[] array;
    private int putIndex;
    private int takeIndex;

    public AtomicRingArray(int capacity) {
        this.capacity = capacity;
        this.array = new Object[capacity];
    }
}
