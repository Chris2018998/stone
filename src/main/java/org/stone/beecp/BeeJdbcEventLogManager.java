/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * jdbc event logs manager interface,at present,only support two kinds of log:{@link BeeJdbcEventLog#Type_Connection_Get}
 * and {@link BeeJdbcEventLog#Type_SQL_Execution}.
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeJdbcEventLogManager {

    //***************************************************************************************************************//
    //                                         1: initialization                                                     //
    //***************************************************************************************************************//

    /**
     * Initializes log manager.
     *
     * @param cacheSize  is capacity size of logs cache
     * @param slowGet    is slow threshold value of connection get,time unit:milliseconds
     * @param slowExec   is slow threshold of sql execution,time unit:milliseconds
     * @param syncHandle is work mode of handler
     * @param handler    is a handler to handle slow logs
     */
    void init(int cacheSize,
              long slowGet, long slowExec,
              boolean syncHandle, BeeJdbcEventLogHandler handler);

    //***************************************************************************************************************//
    //                                         2: logs maintenance                                                   //
    //***************************************************************************************************************//

    /**
     * Clears timeout logs from manager,this method only called by pool timer.
     *
     * @param timeout to clear timeout logs
     */
    void clearTimeout(long timeout);

    /**
     * Clears logs with a given type,this method only called by data source.
     *
     * @param type to clear type matched logs
     */
    List<BeeJdbcEventLog> clear(int type);

    /**
     * Query logs with a given log type.
     *
     * @param type is a log type for logs being gotten
     * @return a result log list of expected type
     */
    List<BeeJdbcEventLog> getLog(int type);

    //***************************************************************************************************************//
    //                                         3: logs collection                                                        //
    //***************************************************************************************************************//

    /**
     * Plugin method executed before target method call to collect some invocation info, for example: method name, method parameters and  so on.
     *
     * @param type        is log type of method call
     * @param method      is method name of call
     * @param parameters  is parameters array of method call,
     * @param preparedSQL is a prepared sql,if current call is a SQL preparation call: {@code Connection.prepareStatement(String,...)} or on {@code Connection.prepareCall(String,...)}
     * @param statement   is a sql execution statement,which may be null
     */
    BeeJdbcEventLog startCall(int type, String method, Object[] parameters, String preparedSQL, Statement statement);


    /**
     * Plugin method executed after method call success to collect call result and other trace info.
     *
     * @param callResult          is result of target method call
     * @param log                 generated from {@link #startCall}method
     * @param preparationTookTime is an elapsed time on {@code Connection.prepareStatement(String,...)} or on {@code Connection.prepareCall(String,...)}
     * @param preparedParameters  is a parameter array of PreparedSQL or CallableSQL
     */
    void endCall(Object callResult, long preparationTookTime, Object[] preparedParameters, BeeJdbcEventLog log);

    /**
     * Plugin method executed after method call failed to collect call exception and other trace info.
     *
     * @param failCause           is result of target method call
     * @param log                 generated from startCall method
     * @param preparationTookTime is an elapsed time on {@code Connection.prepareStatement(String,...)} or on {@code Connection.prepareCall(String,...)}
     * @param preparedParameters  is a parameter array of PreparedSQL or CallableSQL
     */
    void endOnException(Throwable failCause, long preparationTookTime, Object[] preparedParameters, BeeJdbcEventLog log);

    //***************************************************************************************************************//
    //                                         4: statement                                                          //
    //***************************************************************************************************************//

    /**
     * Cancel statement in executing.
     *
     * @param id log id
     */
    void cancelRunningStatement(Object id) throws SQLException;
}
