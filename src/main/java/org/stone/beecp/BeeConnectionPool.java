/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp;

import org.stone.beecp.pool.exception.ConnectionGetInterruptedException;
import org.stone.beecp.pool.exception.ConnectionGetTimeoutException;

import javax.sql.XAConnection;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Connection pool interface
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeConnectionPool {

    /**
     * Pool initializes with a configuration object.
     *
     * @param config contains some items
     * @throws SQLException when initializes fail
     */
    void init(BeeDataSourceConfig config) throws SQLException;

    /**
     * Attempts to get a connection from pool.
     *
     * @return a borrowed connection
     * @throws SQLException                      when fail to create a connection
     * @throws ConnectionGetTimeoutException     when borrower thread wait time out in pool
     * @throws ConnectionGetInterruptedException while borrower waiting is interrupted
     */
    Connection getConnection() throws SQLException;

    /**
     * Attempts to get a XAConnection from pool.
     *
     * @return a borrowed XAConnection
     * @throws SQLException                      when fail to create a xa connection
     * @throws ConnectionGetTimeoutException     when borrower thread wait time out in pool
     * @throws ConnectionGetInterruptedException while borrower waiting is interrupted
     */
    XAConnection getXAConnection() throws SQLException;


    /**
     * Closes all connections in pool and remove them,then change pool state from working to closed.
     */
    void close();

    /**
     * Query pool state whether is closed.
     *
     * @return true that pool is closed
     */
    boolean isClosed();

    /**
     * Enable or disable log print in pool.
     *
     * @param enable is true that print, false not print
     */
    void setPrintRuntimeLog(boolean enable);

    /**
     * Gets monitor vo,which bring out some runtime info of pool.
     *
     * @return monitor of pool
     */
    BeeConnectionPoolMonitorVo getPoolMonitorVo();

    /**
     * Interrupts creating process of connections.
     *
     * @param onlyInterruptTimeout is true,only for timeout creation;otherwise for all creation in processing
     * @return interrupted threads
     */
    Thread[] interruptConnectionCreating(boolean onlyInterruptTimeout);

    /**
     * Closes all connections and removes them from pool.
     *
     * @param forceCloseUsing is an indicator that close borrowed connections immediately,or that close them when them return to pool
     * @throws SQLException when pool closed or in cleaning
     */
    void clear(boolean forceCloseUsing) throws SQLException;

    /**
     * Closes all connections and removes them from pool,then re-initialize pool with new configuration.
     *
     * @param forceCloseUsing is an indicator that close borrowed connections immediately,or that close them when them return to pool
     * @param config          is a new configuration object
     * @throws BeeDataSourceConfigException when check failed on this new configuration
     * @throws SQLException                 when pool closed or in cleaning
     * @throws SQLException                 when pool reinitialize failed
     */
    void clear(boolean forceCloseUsing, BeeDataSourceConfig config) throws SQLException;

}
	
