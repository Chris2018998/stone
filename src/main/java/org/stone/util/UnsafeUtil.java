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
