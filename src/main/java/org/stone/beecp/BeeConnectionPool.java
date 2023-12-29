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
 * Connection pool interface,whose implementation works inside{@link BeeDataSource}
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeConnectionPool {

    /**
     * Method called in class {@link BeeDataSource}to initialize a inner pool instance with a configuration object,
     * which contains some field-level configured items applied into this pool.After initialization,connections borrow
     * request can be allowed pass to pool.
     *
     * @param config object contains sub items works as control parameters in pool
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
     * @return a borrowed connection
     * @throws SQLException when get failed
     */
    Connection getConnection() throws SQLException;

    /**
     * Borrows a XA type connection which supports distributed transaction.The method feature depends XA standard
     * interfaces whether implemented in JDBC Driver.
     *
     * @return a borrowed XAConnection
     * @throws SQLException when get failed 
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
     * Changes on indicator of pool runtime log print
     *
     * @param indicator true,print;false,not print
     */
    void setPrintRuntimeLog(boolean indicator);

    /**
     * Returns a view object contains pool monitor info,such as state,idle,using and so on.
     *
     * @return monitor vo
     */
    BeeConnectionPoolMonitorVo getPoolMonitorVo();

    /**
     * Removes all pooled connections,this method should work under synchronization control,success caller update
     * pool state from {@code ConnectionPoolStatics.POOL_READY} to {@code ConnectionPoolStatics.POOL_CLEARING} and
     * interrupts all blocking waiters to leave from pool.Before completion of clearing,the pool rejects borrowing
     * requests.All idle connections closed immediately and removed,if parameter<h1>forceCloseUsing</h1>is true,
     * do same operation on all using connections like idle,but false,the caller thead waits using return to pool,
     * then close them.Finally,pool state reset to {@code ConnectionPoolStatics.POOL_READY} when done.
     *
     * @param forceCloseUsing is a indicator that direct close or delay close on using connections
     */
    void clear(boolean forceCloseUsing);

    /**
     * Removes all pooled connections and restarts pool with new configuration when the config parameter is not null,
     * the method is similar to the previous{@link #clear}.
     *
     * @param forceCloseUsing is a indicator that direct close or delay close on using connections
     * @param config          is new configuration for pool restarting
     * @throws SQLException when configuration checks failed or pool re-initializes failed
     */
    void clear(boolean forceCloseUsing, BeeDataSourceConfig config) throws SQLException;

}
	
