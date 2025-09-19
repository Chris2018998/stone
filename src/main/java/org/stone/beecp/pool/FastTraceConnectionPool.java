/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp.pool;

import javax.sql.XAConnection;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * JDBC Trace Connection Pool Implementation
 *
 * @author Chris Liao
 * @version 1.0
 */

public final class FastTraceConnectionPool extends FastConnectionPool {
    private static final String GetConnection = "FastTraceConnectionPool.getConnection()";
    private static final String GetXAConnection = "FastTraceConnectionPool.getXAConnection()";

    public Connection getConnection() throws SQLException {
        long startTime = System.currentTimeMillis();
        Object key = conTracker.genKey();
        try {
            conTracker.beforeGetConnection(key, GetConnection, startTime);
            Connection con = super.getConnection();
            conTracker.afterGetConnection(key, GetConnection, startTime, System.currentTimeMillis());
            return con;
        } catch (SQLException e) {
            conTracker.onException(key, GetConnection, startTime, System.currentTimeMillis(), e, null, null);
            throw e;
        }
    }

    public XAConnection getXAConnection() throws SQLException {
        long startTime = System.currentTimeMillis();
        Object key = conTracker.genKey();
        try {
            conTracker.beforeGetConnection(key, GetXAConnection, startTime);
            XAConnection con = super.getXAConnection();
            conTracker.afterGetConnection(key, GetXAConnection, startTime, System.currentTimeMillis());
            return con;
        } catch (SQLException e) {
            conTracker.onException(key, GetXAConnection, startTime, System.currentTimeMillis(), e, null, null);
            throw e;
        }
    }
}
