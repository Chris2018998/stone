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
     * check firstly,if failed then throws cause exception. After initialization,connections borrow request can
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
     * Method invocation to shut down pool and need supply thead-safe control on this call,just only one thread to
     * success do it when concurrent,others exit from method immediately and coming borrow requests rejected when
     * pool in closing state or in closed state.
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
     * @param forceCloseUsing
     * @throws SQLException
     */
    void clear(boolean forceCloseUsing) throws SQLException;

    /**
     * @param forceCloseUsing
     * @param config
     * @throws SQLException
     */
    void clear(boolean forceCloseUsing, BeeDataSourceConfig config) throws SQLException;

}
	
