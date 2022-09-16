/*
 * Copyright(C) Chris2018998
 *
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * Atomic Unsafe Util
 *
 * @author Chris Liao
 * @version 1.0
 */
public class UnsafeUtil {
    private static Unsafe unsafe;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            unsafe = (Unsafe) theUnsafe.get(null);
        } catch (Exception e) {
            throw new Error("Failed to get Unsafe", e);
        }
    }

    public static Unsafe getUnsafe() {
        return UnsafeUtil.unsafe;
    }
}
