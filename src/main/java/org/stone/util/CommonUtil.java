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

/**
 * common util
 *
 * @author Chris Liao
 * @version 1.0
 */

public class CommonUtil {

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
