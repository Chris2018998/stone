/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.tools.unsafe;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * Atomic Unsafe Util
 *
 * @author Chris Liao
 * @version 1.0
 */
public final class UnsafeAdaptorSunMiscImpl implements UnsafeAdaptor {
    private static final Unsafe U;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            U = (Unsafe) theUnsafe.get(null);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    //****************************************************************************************************************//
    //                                            field offset                                                        //
    //****************************************************************************************************************//
    public final long objectFieldOffset(Field field) {
        if (!field.isAccessible()) field.setAccessible(true);
        return U.objectFieldOffset(field);
    }

    public final long staticFieldOffset(Field field) {
        if (!field.isAccessible()) field.setAccessible(true);
        return U.staticFieldOffset(field);
    }

    public final Object staticFieldBase(Field field) {
        if (!field.isAccessible()) field.setAccessible(true);
        return U.staticFieldBase(field);
    }

    //****************************************************************************************************************//
    //                                            volatile(int)                                                       //
    //****************************************************************************************************************//
    public final int getIntVolatile(Object object, long offset) {
        return U.getIntVolatile(object, offset);
    }

    public final void putIntVolatile(Object object, long offset, int update) {
        U.putIntVolatile(object, offset, update);
    }

    public final void putOrderedInt(Object object, long offset, int update) {
        U.putOrderedInt(object, offset, update);
    }

    public final boolean compareAndSwapInt(Object object, long offset, int expect, int update) {
        return U.compareAndSwapInt(object, offset, expect, update);
    }

    //****************************************************************************************************************//
    //                                            volatile(long)                                                      //
    //****************************************************************************************************************//
    public final long getLongVolatile(Object object, long offset) {
        return U.getLongVolatile(object, offset);
    }

    public final void putLongVolatile(Object object, long offset, long update) {
        U.putLongVolatile(object, offset, update);
    }

    public final void putOrderedLong(Object object, long offset, long update) {
        U.putOrderedLong(object, offset, update);
    }

    public final boolean compareAndSwapLong(Object object, long offset, long expect, long update) {
        return U.compareAndSwapLong(object, offset, expect, update);
    }

    //****************************************************************************************************************//
    //                                            volatile(Object)                                                    //
    //****************************************************************************************************************//
    public final Object getObjectVolatile(Object object, long offset) {
        return U.getObjectVolatile(object, offset);
    }

    public final void putObjectVolatile(Object object, long offset, Object update) {
        U.putObjectVolatile(object, offset, update);
    }

    public final void putOrderedObject(Object object, long offset, Object update) {
        U.putOrderedObject(object, offset, update);
    }

    public final boolean compareAndSwapObject(Object object, long offset, Object expect, Object update) {
        return U.compareAndSwapObject(object, offset, expect, update);
    }
}
