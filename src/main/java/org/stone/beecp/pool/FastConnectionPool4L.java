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

import org.stone.beecp.BeeJdbcCallLog;

import javax.sql.XAConnection;
import java.sql.Connection;
import java.sql.SQLException;

import static org.stone.beecp.BeeJdbcCallLog.Type_Get_Connection;

/**
 * JDBC Trace Connection Pool Implementation
 *
 * @author Chris Liao
 * @version 1.0
 */

public final class FastConnectionPool4L extends FastConnectionPool {
    private static final String GetConnection = "FastConnectionPool4L.getConnection()";
    private static final String GetXAConnection = "FastConnectionPool4L.getXAConnection()";

    public Connection getConnection() throws SQLException {
        if (this.isEnabledJdbcCallLogCollector()) {
            BeeJdbcCallLog log = logCollector.startCall(Type_Get_Connection, GetConnection, null, null, null);
            try {
                Connection con = super.getConnection();
                logCollector.endCall(con, 0L, null, log);
                return con;
            } catch (SQLException e) {
                logCollector.endOnException(e, 0L, null, log);
                throw e;
            }
        } else {
            return super.getConnection();
        }
    }

    public XAConnection getXAConnection() throws SQLException {
        if (this.isEnabledJdbcCallLogCollector()) {
            BeeJdbcCallLog log = logCollector.startCall(Type_Get_Connection, GetXAConnection, null, null, null);
            try {
                XAConnection con = super.getXAConnection();
                logCollector.endCall(con, 0L, null, log);
                return con;
            } catch (SQLException e) {
                logCollector.endOnException(e, 0L, null, log);
                throw e;
            }
        } else {
            return super.getXAConnection();
        }
    }
}
