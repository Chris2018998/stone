/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.XAConnection;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.stone.tools.CommonUtil.objectEquals;

/**
 * @author Chris Liao
 * @version 1.0
 */
public class TestUtil {
    private final static Logger log = LoggerFactory.getLogger(TestUtil.class);

    public static void assertError(String message) {
        throw new AssertionError(message);
    }

    public static void assertError(String message, Object expect, Object current) {
        if (!objectEquals(expect, current))
            throw new AssertionError(String.format(message, String.valueOf(expect), String.valueOf(current)));
    }

    public static Object getFieldValue(Object ob, String fieldName) throws Exception {
        Field field = ob.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(ob);
    }

    public static Object invokeMethod(Object ob, String methodName) {
        try {
            Method method = ob.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(ob);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void oclose(ResultSet r) {
        try {
            r.close();
        } catch (Throwable e) {
            log.warn("Warning:Error at closing resultSet:", e);
        }
    }

    public static void oclose(Statement s) {
        try {
            s.close();
        } catch (Throwable e) {
            log.warn("Warning:Error at closing statement:", e);
        }
    }

    public static void oclose(Connection c) {
        try {
            c.close();
        } catch (Throwable e) {
            log.warn("Warning:Error at closing connection:", e);
        }
    }

    public static void oclose(XAConnection c) {
        try {
            c.close();
        } catch (Throwable e) {
            log.warn("Warning:Error at closing resultSet:", e);
        }
    }
}
