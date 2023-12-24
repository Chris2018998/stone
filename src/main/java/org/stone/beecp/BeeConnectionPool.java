/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
 */
package org.stone.beecp;

import javax.sql.XAConnection;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Connection pool interface,whose implementation works inside BeeDataSource
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeConnectionPool {

    /**
     * Pool initialize with a configuration object contains some sub items,but before apply them into pool and
     * check firstly,if failed then throws cause exception.After initialization,connections borrow request can
     * allow pass to pool
     *
     * @param config pool configuration object
     * @throws SQLException when configuration check failed or pool initialize failed
     */
    void init(BeeDataSourceConfig config) throws SQLException;

    /**
     * Method call to borrow an idle connection from pool,but if not exists idle,borrower thread blocking in pool with
     * specified {@code maxWait} time in datasource configuration.When other borrowers release their used connections
     * to pool,then wake up a waiter thread to get or transfer directly the released connection to a waiter via cas mode,
     * and the target waiter hold success,then end waiting and leave from pool.if not get one util elapsed time reach
     * {@code maxWait}value,waiters leave from pool with a timeout sql-typed exception.Borrowed out connections can't be
     * borrowed again,means that a connection just only hold by a thread at any time.
     *
     * @return a idle connection
     * @throws SQLException when wait timeout or interrupted while wait in pool
     */
    Connection getConnection() throws SQLException;

    /**
     * Borrows a xa type connection which supports distributed transaction.The method feature depends XA standard
     * interfaces whether implemented in JDBC Driver.The working process logic under this method is similar to method
     * {@code getConnection},a resulted XA wrapper built on it by JDBC driver when a pooled connection borrowed.
     *
     * @return a borrowed XAConnection
     * @throws SQLException when wait timeout or interrupted while wait in pool
     */
    XAConnection getXAConnection() throws SQLException;

    /**
     * Method invocation to shut down pool,there is a thead-safe control,only one thread success call
     * it at concurrent,the pool state mark to be closed and rejects coming requests with a exception.
     * This method support duplicated call when in closed state,do nothing,just return.
     */
    void close();

    /**
     * test pool whether in closed state
     *
     * @return a boolean,true closed
     */
    boolean isClosed();

    /**
     * enable runtime log to be printed or not
     *
     * @param indicator true print,false not print
     */
    void setPrintRuntimeLog(boolean indicator);

    /**
     * @return monitor vo
     */
    BeeConnectionPoolMonitorVo getPoolMonitorVo();

    /**
     * Clears all pooled connections,a thread-safe control should be added around this method call.Coming requests rejected
     * when pool in closed state,after clearing,pool state will reset to be ready for requests.Logic of clearing is below
     * 1: Idle connections close directly and remove
     * 2: Using connections close directly and remove if the parameter {@code forceCloseUsing}is true
     * 2.1: Delay specified {@code delayTimeForNextClear} to check using connections already return to pool,if true close them
     *
     * @param forceCloseUsing a indicator to close using connections
     */
    void clear(boolean forceCloseUsing);

    /**
     * Clears all pooled connections and apply a new configuration to pool when parameter config is not null
     *
     * @param forceCloseUsing a indicator to close using connections
     * @param config          which apply to pool as new configuration when not be null
     * @throws SQLException when apply configuration failed
     */
    void clear(boolean forceCloseUsing, BeeDataSourceConfig config) throws SQLException;

}
	
