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

    //***************************************************************************************************************//
    //                1: pool initialize method(1)                                                                   //                                                                                  //
    //***************************************************************************************************************//

    /**
     * Pool initialize with a configuration object contains some sub items,but before apply them into pool and
     * check them firstly,if failed then throws cause exception. After initialization,connections borrowed request
     * can allow pass to pool
     *
     * @param config pool configuration object
     * @throws SQLException when configuration check failed or failure in initialization
     */
    void init(BeeDataSourceConfig config) throws SQLException;

    //***************************************************************************************************************//
    //                2: objects methods(2)                                                                          //                                                                                  //
    //***************************************************************************************************************//

    /**
     * borrows out an idle connection from pool,but if not get a idle connection,request thread waits in pool inner
     * queue util a transferred one from other thread release or timeout.There exist using flags marked in borrowed
     * connections,means that one connection only borrowed out by one thread at any time.
     *
     * @return a idle connection
     * @throws SQLException when wait timeout or interrupted while wait in pool
     */
    Connection getConnection() throws SQLException;

    /**
     * borrows a XAConnection which apply on distributed transaction,if driver not support this feature,a local impl
     * provided
     *
     * @return a borrowed XAConnection
     * @throws SQLException when wait timeout or interrupted while wait in pool
     */
    XAConnection getXAConnection() throws SQLException;

    //***************************************************************************************************************//
    //                3: Pool runtime maintain methods(6)                                                            //                                                                                  //
    //***************************************************************************************************************//

    /**
     * Method invocation to shut down pool and need supply thead-safe control on its call,just only one thread to
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
	
