/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beecp.pool;

import org.stone.beecp.BeeDataSourceConfig;

import javax.sql.XAConnection;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Connection pool interface
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface ConnectionPool {

    //initialize pool with configuration
    void init(BeeDataSourceConfig config) throws SQLException;

    //borrow a connection from pool
    Connection getConnection() throws SQLException;

    //borrow a connection from pool
    XAConnection getXAConnection() throws SQLException;

    //recycle one pooled Connection
    void recycle(PooledConnection p);

    //close pool
    void close();

    //check pool is closed
    boolean isClosed();

    //enable Runtime Log
    void setPrintRuntimeLog(boolean indicator);

    //get pool monitor vo
    ConnectionPoolMonitorVo getPoolMonitorVo();

    //restart all connections from pool,forceCloseUsingOnClear is true,then close using connection directly
    void restart(boolean forceCloseUsing) throws SQLException;

    //restart all connections from pool,forceCloseUsingOnClear is true,then close using connection directly
    void restart(boolean forceCloseUsing, BeeDataSourceConfig config) throws SQLException;

}
	
