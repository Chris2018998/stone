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
 * An interface,its implementation is used to collect logs of connection get and logs of SQL execution.
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeJdbcCallLogCollector {

    //***************************************************************************************************************//
    //                                         1: initialization                                                     //
    //***************************************************************************************************************//

    /**
     * initialize log collector.
     *
     * @param cacheSize    is capacity of logs cache
     * @param slowGet      is slow threshold value of connection get,time unit:milliseconds
     * @param slowExec     is slow threshold of sql execution,time unit:milliseconds
     * @param listenInSync is work mode of listener
     * @param listener     is a log listener
     */
    void init(int cacheSize,
              long slowGet, long slowExec,
              boolean listenInSync, BeeJdbcCallLogListener listener);

    //***************************************************************************************************************//
    //                                         2: logs maintain                                                      //
    //***************************************************************************************************************//

    /**
     * Clear timeout logs from collector.
     *
     * @param timeout to check timeout logs
     */
    void clear(long timeout);

    /**
     * Query and get logs with given type.
     *
     * @param type is log type
     * @return a list of logs
     */
    List<BeeJdbcCallLog> getLog(int type);

    //***************************************************************************************************************//
    //                                         3: logs record                                                        //
    //***************************************************************************************************************//

    /**
     * Plugin method is executed at front of proxy methods to generate a start log object.
     *
     * @param type        is method call type
     * @param method      is method name,for example:getConnection()
     * @param parameters  is an array of method parameters,which may be null
     * @param preparedSQL is a prepared sql,which may be null
     * @param statement   is a sql statement,which may be null
     */
    BeeJdbcCallLog startCall(int type, String method, Object[] parameters, String preparedSQL, Statement statement);

    /**
     * Plugin method is executed at end of proxy methods to update result and attach to a log object.
     *
     * @param callResult          is result of target method call
     * @param log                 generated from {@link #startCall}method
     * @param preparationTookTime is an elapsed time on {@code Connection.prepareStatement(String,...)} or on {@code Connection.prepareCall(String,...)}
     * @param preparedParameters  is a parameter array of PreparedSQL or CallableSQL
     */
    void endCall(Object callResult, long preparationTookTime, Object[] preparedParameters, BeeJdbcCallLog log);

    /**
     * Plugin method is executed at exception catch block of proxy methods to update exception and attach to a log object.
     *
     * @param failCause           is result of target method call
     * @param log                 generated from startCall method
     * @param preparationTookTime is an elapsed time on {@code Connection.prepareStatement(String,...)} or on {@code Connection.prepareCall(String,...)}
     * @param preparedParameters  is a parameter array of PreparedSQL or CallableSQL
     */
    void endOnException(Throwable failCause, long preparationTookTime, Object[] preparedParameters, BeeJdbcCallLog log);

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
