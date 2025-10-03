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
import java.io.Closeable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Connection pool interface.
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeConnectionPool extends Closeable {

    /**
     * Pool initializes with a configuration object.
     *
     * @param config is a configuration object defines some items can be applied for pool
     * @throws BeeDataSourceConfigException when configuration check fail
     * @throws SQLException                 when fail to create initialization connection
     */
    void init(BeeDataSourceConfig config) throws SQLException;

    /**
     * Attempts to get a connection from pool.
     *
     * @return a borrowed connection
     * @throws SQLException                      when fail to create a connection
     * @throws ConnectionGetTimeoutException     when wait timeout in pool
     * @throws ConnectionGetInterruptedException while interruption occurred during waiting
     */
    Connection getConnection() throws SQLException;

    /**
     * Attempts to get a XAConnection from pool.
     *
     * @return a borrowed XAConnection
     * @throws SQLException                      when fail to create a xa connection
     * @throws ConnectionGetTimeoutException     when wait timeout in pool
     * @throws ConnectionGetInterruptedException while interruption occurred during waiting
     */
    XAConnection getXAConnection() throws SQLException;

    /**
     * Interrupts connections creation in blocking.
     *
     * @param onlyInterruptTimeout is true that only interrupts timeout creation,false that interrupts all creation in blocking
     * @return interrupted threads
     */
    Thread[] interruptConnectionCreating(boolean onlyInterruptTimeout);

    /**
     * Physically closes all connections and removes them from pool. Idle Connections are closed immediately when call
     * this method; if exists borrowed connections,pool close them according to the method parameter value.
     *
     * @param forceRecycleBorrowed is true that pool close immediately borrowed connections by force; false that close
     *                             operation after them return to pool.
     * @throws SQLException when pool is closed or in clearing
     */
    void clear(boolean forceRecycleBorrowed) throws SQLException;

    /**
     * Physically closes all connections and removes them from pool,then re-initializes with a new configuration.
     *
     * @param forceRecycleBorrowed is true that pool close immediately borrowed connections by force; false that close
     *                             operation after them return to pool.
     * @param config               is a new configuration object for reinitialization
     * @throws BeeDataSourceConfigException when configuration check fail
     * @throws SQLException                 when pool is closed or in clearing
     * @throws SQLException                 when pool reinitialize fail
     */
    void clear(boolean forceRecycleBorrowed, BeeDataSourceConfig config) throws SQLException;

    /**
     * Gets runtime monitoring object of pool,refer to {@link BeeConnectionPoolMonitorVo}.
     *
     * @return monitoring object of pool
     */
    BeeConnectionPoolMonitorVo getPoolMonitorVo();


    /**
     * Shutdown pool
     */
    void close();

    /**
     * Queries pool state whether is closed.
     *
     * @return true when pool is closed
     */
    boolean isClosed();

    /**
     * Query logs print state whether in being enabled.
     *
     * @return boolean true is enabled,false is disabled
     */
    boolean isEnabledLogPrint();

    /**
     * A switch to enable or disable pool work logs print.
     *
     * @param enable is true that log print is enabled, false is not print
     */
    void enableLogPrint(boolean enable);

    /**
     * Queries logs collector whether being enabled.
     *
     * @return boolean true is enabled,false is disabled
     */
    boolean isEnabledJdbcCallLogCollector();

    /**
     * Method call to enable log collector and disable it.
     *
     * @param enable is true that let configured log collector works,false is that disable it
     */
    void enableJdbcCallLogCollector(boolean enable);

    /**
     * Clear All logs in log collector.
     */
    void clearJdbcCallLog();

    /**
     * Get Jdbc logs with a give type.
     *
     * @param type is log type to query
     */
    List<BeeJdbcCallLog> getJdbcCallLog(int type);

}
	
