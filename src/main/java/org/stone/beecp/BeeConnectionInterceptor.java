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
 * A plugin interface,whose implementation used to intercept some operation on connection,statement
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeConnectionInterceptor {

    //***************************************************************************************************************//
    //                              1: keys Generation                                                               //
    //***************************************************************************************************************//

    /**
     * generate an operation key.
     *
     * @return a key
     */
    Object genKey();

    //***************************************************************************************************************//
    //                              2: event on BeeConnectionPool get methods(getConnection/getXAConnection)          //
    //***************************************************************************************************************//

    /**
     * Records a start event of connection getting and XAConnection getting
     *
     * @param opKey           is an operation key
     * @param startTime       operation start time,unit:milliseconds
     * @param methodSignature is name of a trace method
     */
    void beforeGetConnection(Object opKey, String methodSignature, long startTime);

    /**
     * Record a success event of connection getting or XAConnection getting
     *
     * @param opKey           is an operation key
     * @param methodSignature is name of a trace method
     * @param startTime       operation start time,unit:milliseconds
     * @param endTime         trace end time,unit:milliseconds
     */
    void afterGetConnection(Object opKey, String methodSignature, long startTime, long endTime);


    //***************************************************************************************************************//
    //                              3: trace on connection preparation methods(prepareStatement,prepareCall)         //
    //***************************************************************************************************************//

    /**
     * Records a start event before invocation on {@code Connection.prepareStatement(String,...)} method of a connection.
     *
     * @param opKey           is an operation key
     * @param methodSignature is name of a trace method
     * @param startTime       operation start time,unit:milliseconds
     * @param sql             is target preparation sql
     */
    void beforePrepareSQL(Object opKey, String methodSignature, long startTime, String sql);

    /**
     * Record a success event after invocation on {@code Connection.prepareStatement(String,...)} method of a connection.
     *
     * @param opKey           is an operation key
     * @param methodSignature is name of a trace method
     * @param startTime       operation start time,unit:milliseconds
     * @param endTime         trace end time,unit:milliseconds
     * @param sql             is target preparation sql
     */
    void afterPrepareSQL(Object opKey, String methodSignature, long startTime, long endTime, String sql);

    //***************************************************************************************************************//
    //                              4: Trace on PreparedStatement,CallableStatement                                  //
    //***************************************************************************************************************//

    /**
     * Records a start event before invocation on {@code PreparedStatement.executeXX()} method of a preparedStatement.
     * method list[PreparedStatement.execute(),PreparedStatement.executeQuery(),PreparedStatement.executeUpdate(),PreparedStatement.executeLargeUpdate()]
     *
     * @param opKey           is an operation key
     * @param methodSignature is name of a trace method
     * @param startTime       operation start time,unit:milliseconds
     * @param preparedKey     is a trace key generated during invocation of prepareStatement and of prepareCall
     * @param sql             is a target execution sql
     */
    void beforeExecutePreparedSQL(Object opKey, String methodSignature, long startTime, Object preparedKey, String sql);

    /**
     * Record a success event after invocation on {@code PreparedStatement.executeXX()} method of a PreparedStatement.
     *
     * @param opKey           is an operation key
     * @param methodSignature is name of a trace method
     * @param startTime       operation start time,unit:milliseconds
     * @param endTime         trace end time,unit:milliseconds
     * @param preparedKey     is a trace key generated during invocation of prepareStatement and of prepareCall
     * @param sql             is a target execution sql
     */
    void afterExecutePreparedSQL(Object opKey, String methodSignature, long startTime, long endTime, Object preparedKey, String sql);


    //***************************************************************************************************************//
    //                              5: Trace on statement sql execution methods                                      //
    //***************************************************************************************************************//

    /**
     * Records a start event before invocation on {@code Statement.executeXX(String,...)} method of a Statement.
     * method list[Statement.execute(String),Statement.executeQuery(String),Statement.executeUpdate(String),Statement.executeLargeUpdate(String)]
     *
     * @param opKey           is an operation key
     * @param methodSignature is name of a trace method
     * @param startTime       operation start time,unit:milliseconds
     * @param sql             is a target execution sql
     */
    void beforeExecuteSQL(Object opKey, String methodSignature, long startTime, String sql);

    /**
     * Record a success event after invocation on {@code Statement.executeXX(String,...)} method of a Statement.
     *
     * @param opKey           is an operation key
     * @param methodSignature is name of a trace method
     * @param startTime       operation start time,unit:milliseconds
     * @param endTime         trace end time,unit:milliseconds
     * @param sql             is a target execution sql
     */
    void afterExecuteSQL(Object opKey, String methodSignature, long startTime, long endTime, String sql);

    //***************************************************************************************************************//
    //                              6:   Exception                                                                   //
    //***************************************************************************************************************//

    /**
     * Record an exception event
     *
     * @param opKey           is an operation key
     * @param methodSignature is name of a trace method
     * @param startTime       operation start time,unit:milliseconds
     * @param endTime         trace end time,unit:milliseconds
     * @param e               is fail exception from trace method
     * @param preparedKey     is a sql preparation trace key
     * @param sql             is a failure sql,such as fail preparation or fail execution
     */
    void onException(Object opKey, String methodSignature, long startTime, long endTime, Throwable e, Object preparedKey, String sql);

}
