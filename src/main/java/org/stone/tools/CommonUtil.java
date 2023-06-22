/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.tools;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * common util
 *
 * @author Chris Liao
 * @version 1.0
 */

public class CommonUtil {
    public static final int NCPU = Runtime.getRuntime().availableProcessors();
    public static final int maxTimedSpins = (NCPU < 2) ? 0 : 32;
    public static final int maxUntimedSpins = maxTimedSpins * 16;
    public static final long spinForTimeoutThreshold = 1023L;
    public static final sun.misc.Unsafe UNSAFE;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe) theUnsafe.get(null);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public static long objectFieldOffset(Class clazz, String fieldName) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        if (field.isAccessible()) field.setAccessible(true);
        return UNSAFE.objectFieldOffset(field);
    }

    public static String trimString(String value) {
        return value == null ? null : value.trim();
    }

    public static boolean objectEquals(Object a, Object b) {
        return a == b || a != null && a.equals(b);
    }

    public static boolean isBlank(String str) {
        if (str == null) return true;
        for (int i = 0, l = str.length(); i < l; ++i) {
            if (!Character.isWhitespace((int) str.charAt(i)))
                return false;
        }
        return true;
    }
}
