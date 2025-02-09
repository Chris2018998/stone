/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp.other;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stone.beeop.BeeObjectHandle;

import javax.sql.XAConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * @author Chris Liao
 * @version 1.0
 */
public class TestUtil {
    private static final Logger log = LoggerFactory.getLogger(TestUtil.class);

    public static void oclose(ResultSet r) {
        try {
            r.close();
        } catch (Throwable e) {
            e.printStackTrace();
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

    public static void oclose(BeeObjectHandle h) {
        try {
            h.close();
        } catch (Throwable e) {
            log.warn("Warning:Error at closing object handle:", e);
        }
    }
}
