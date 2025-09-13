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

/**
 * A plugin interface to trace some operation on connection,statement
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeConnectionTracker {

    //***************************************************************************************************************//
    //                              1: Generates unique trace keys                                                   //
    //***************************************************************************************************************//

    /**
     * generate a key for operation trace.
     *
     * @return a unique key
     */
    Object genTraceKey();

    //***************************************************************************************************************//
    //                              2: Trace on BeeConnectionPool get methods(getConnection/getXAConnection)                //
    //***************************************************************************************************************//

    /**
     * Records a start event of connection getting and XAConnection getting
     *
     * @param traceKey        is operation trace key
     * @param startTime       trace start time,unit:milliseconds
     * @param methodSignature is name of a trace method
     */
    void beforeGetConnection(Object traceKey, String methodSignature, long startTime);

    /**
     * Record a success event of connection getting or XAConnection getting
     *
     * @param traceKey        is operation trace key
     * @param methodSignature is name of a trace method
     * @param startTime       trace start time,unit:milliseconds
     * @param endTime         trace end time,unit:milliseconds
     */
    void afterGetConnection(Object traceKey, String methodSignature, long startTime, long endTime);


    //***************************************************************************************************************//
    //                              3: trace on connection preparation methods(prepareStatement,prepareCall)        //
    //***************************************************************************************************************//

    /**
     * Records a start event before invocation on {@code Connection.prepareStatement(String,...)} method of a connection.
     *
     * @param traceKey        is operation trace key
     * @param methodSignature is name of a trace method
     * @param startTime       trace start time,unit:milliseconds
     * @param sql             is target preparation sql
     */
    void beforePrepareSQL(Object traceKey, String methodSignature, long startTime, String sql);

    /**
     * Record a success event after invocation on {@code Connection.prepareStatement(String,...)} method of a connection.
     *
     * @param traceKey        is operation trace key
     * @param methodSignature is name of a trace method
     * @param startTime       trace start time,unit:milliseconds
     * @param endTime         trace end time,unit:milliseconds
     * @param sql             is target preparation sql
     */
    void afterPrepareSQL(Object traceKey, String methodSignature, long startTime, long endTime, String sql);

    //***************************************************************************************************************//
    //                              4: Trace on PreparedStatement,CallableStatement                                  //
    //***************************************************************************************************************//

    /**
     * Records a start event before invocation on {@code PreparedStatement.executeXX()} method of a preparedStatement.
     * method list[PreparedStatement.execute(),PreparedStatement.executeQuery(),PreparedStatement.executeUpdate(),PreparedStatement.executeLargeUpdate()]
     *
     * @param traceKey        is operation trace key
     * @param methodSignature is name of a trace method
     * @param startTime       trace start time,unit:milliseconds
     * @param preparedKey     is a trace key generated during invocation of prepareStatement and of prepareCall
     * @param sql             is a target execution sql
     */
    void beforeExecutePreparedSQL(Object traceKey, String methodSignature, long startTime, Object preparedKey, String sql);

    /**
     * Record a success event after invocation on {@code PreparedStatement.executeXX()} method of a PreparedStatement.
     *
     * @param traceKey        is operation trace key
     * @param methodSignature is name of a trace method
     * @param startTime       trace start time,unit:milliseconds
     * @param endTime         trace end time,unit:milliseconds
     * @param preparedKey     is a trace key generated during invocation of prepareStatement and of prepareCall
     * @param sql             is a target execution sql
     */
    void afterExecutePreparedSQL(Object traceKey, String methodSignature, long startTime, long endTime, Object preparedKey, String sql);


    //***************************************************************************************************************//
    //                              5: Trace on statement sql execution methods                                      //
    //***************************************************************************************************************//

    /**
     * Records a start event before invocation on {@code Statement.executeXX(String,...)} method of a Statement.
     * method list[Statement.execute(String),Statement.executeQuery(String),Statement.executeUpdate(String),Statement.executeLargeUpdate(String)]
     *
     * @param traceKey        is operation trace key
     * @param methodSignature is name of a trace method
     * @param startTime       trace start time,unit:milliseconds
     * @param sql             is a target execution sql
     */
    void beforeExecuteSQL(Object traceKey, String methodSignature, long startTime, String sql);

    /**
     * Record a success event after invocation on {@code Statement.executeXX(String,...)} method of a Statement.
     *
     * @param traceKey        is operation trace key
     * @param methodSignature is name of a trace method
     * @param startTime       trace start time,unit:milliseconds
     * @param endTime         trace end time,unit:milliseconds
     * @param sql             is a target execution sql
     */
    void afterExecuteSQL(Object traceKey, String methodSignature, long startTime, long endTime, String sql);

    //***************************************************************************************************************//
    //                              6:   Exception                                                                   //
    //***************************************************************************************************************//

    /**
     * Record an exception event
     *
     * @param traceKey        is operation trace key
     * @param methodSignature is name of a trace method
     * @param startTime       trace start time,unit:milliseconds
     * @param endTime         trace end time,unit:milliseconds
     * @param e               is fail exception from trace method
     * @param preparedKey     is a sql preparation trace key
     * @param sql             is a failure sql,such as fail preparation or fail execution
     */
    void onException(Object traceKey, String methodSignature, long startTime, long endTime, Throwable e, Object preparedKey, String sql);

}
