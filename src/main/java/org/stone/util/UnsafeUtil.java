/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * Atomic Unsafe Util
 *
 * @author Chris Liao
 * @version 1.0
 */
public class UnsafeUtil {
    private static Unsafe u;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            u = (Unsafe) theUnsafe.get(null);
        } catch (Exception e) {
            throw new Error("Failed to get Unsafe", e);
        }
    }

    public static Unsafe getUnsafe() {
        return UnsafeUtil.u;
    }

    //****************************************************************************************************************//
    //                                            field offset                                                        //
    //****************************************************************************************************************//
    public static long objectFieldOffset(Class beanClass, String fieldName) throws Exception {
        return u.objectFieldOffset(beanClass.getDeclaredField(fieldName));
    }

    public static long staticFieldOffset(Class beanClass, String fieldName) throws Exception {
        return u.staticFieldOffset(beanClass.getDeclaredField(fieldName));
    }

    public static Object staticFieldBase(Class beanClass, String fieldName) throws Exception {
        return u.staticFieldBase(beanClass.getDeclaredField(fieldName));
    }

    //****************************************************************************************************************//
    //                                            volatile(int)                                                       //
    //****************************************************************************************************************//
    public static int getIntVolatile(Object object, long offset) {
        return u.getIntVolatile(object, offset);
    }

    public static void putIntVolatile(Object object, long offset, int update) {
        u.putIntVolatile(object, offset, update);
    }

    public static boolean compareAndSwapInt(Object object, long offset, int expect, int update) {
        return u.compareAndSwapInt(object, offset, expect, update);
    }

    //****************************************************************************************************************//
    //                                            volatile(long)                                                       //
    //****************************************************************************************************************//
    public static long getLongVolatile(Object object, long offset) {
        return u.getLongVolatile(object, offset);
    }

    public static void putLongVolatile(Object object, long offset, long update) {
        u.putLongVolatile(object, offset, update);
    }

    public static boolean compareAndSwapLong(Object object, long offset, long expect, long update) {
        return u.compareAndSwapLong(object, offset, expect, update);
    }

    //****************************************************************************************************************//
    //                                            volatile(Object)                                                    //
    //****************************************************************************************************************//
    public static Object getObjectVolatile(Object object, long offset) {
        return u.getObjectVolatile(object, offset);
    }

    public static void putObjectVolatile(Object object, long offset, Object update) {
        u.putObjectVolatile(object, offset, update);
    }

    public static boolean compareAndSwapObject(Object object, long offset, Object expect, Object update) {
        return u.compareAndSwapObject(object, offset, expect, update);
    }
}
