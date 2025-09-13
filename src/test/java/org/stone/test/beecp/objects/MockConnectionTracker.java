/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stone.beecp.BeeConnectionTracker;

import java.util.UUID;

/**
 * A simple tracker implementation
 *
 * @author Chris Liao
 */

public class MockConnectionTracker implements BeeConnectionTracker {
    private final Logger Log = LoggerFactory.getLogger(MockConnectionTracker.class);
    //***************************************************************************************************************//
    //                              1: Generates unique trace keys                                                   //
    //***************************************************************************************************************//

    /**
     * generate a key for operation trace.
     *
     * @return a unique key
     */
    public Object genTraceKey() {
        return UUID.randomUUID();
    }

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
    public void beforeGetConnection(Object traceKey, String methodSignature, long startTime) {
        Log.info("beforeGetConnection");
    }

    /**
     * Record a success event of connection getting or XAConnection getting
     *
     * @param traceKey        is operation trace key
     * @param methodSignature is name of a trace method
     * @param startTime       trace start time,unit:milliseconds
     * @param endTime         trace end time,unit:milliseconds
     */
    public void afterGetConnection(Object traceKey, String methodSignature, long startTime, long endTime) {
        Log.info("afterGetConnection:{}", (endTime - startTime));
    }


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
    public void beforePrepareSQL(Object traceKey, String methodSignature, long startTime, String sql) {
        Log.info("beforePrepareSQL:{}", sql);
    }

    /**
     * Record a success event after invocation on {@code Connection.prepareStatement(String,...)} method of a connection.
     *
     * @param traceKey        is operation trace key
     * @param methodSignature is name of a trace method
     * @param startTime       trace start time,unit:milliseconds
     * @param endTime         trace end time,unit:milliseconds
     * @param sql             is target preparation sql
     */
    public void afterPrepareSQL(Object traceKey, String methodSignature, long startTime, long endTime, String sql) {
        Log.info("afterPrepareSQL:{},time:{}", sql, (endTime - startTime));
    }

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
    public void beforeExecutePreparedSQL(Object traceKey, String methodSignature, long startTime, Object preparedKey, String sql) {
        Log.info("beforeExecutePreparedSQL:{}", sql);
    }

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
    public void afterExecutePreparedSQL(Object traceKey, String methodSignature, long startTime, long endTime, Object preparedKey, String sql) {
        Log.info("afterExecutePreparedSQL:{},time:{}", sql, (endTime - startTime));
    }


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
    public void beforeExecuteSQL(Object traceKey, String methodSignature, long startTime, String sql) {
        Log.info("beforeExecuteSQL:{}", sql);
    }

    /**
     * Record a success event after invocation on {@code Statement.executeXX(String,...)} method of a Statement.
     *
     * @param traceKey        is operation trace key
     * @param methodSignature is name of a trace method
     * @param startTime       trace start time,unit:milliseconds
     * @param endTime         trace end time,unit:milliseconds
     * @param sql             is a target execution sql
     */
    public void afterExecuteSQL(Object traceKey, String methodSignature, long startTime, long endTime, String sql) {
        Log.info("afterExecuteSQL:{},time:{}", sql, (endTime - startTime));
    }

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
    public void onException(Object traceKey, String methodSignature, long startTime, long endTime, Throwable e, Object preparedKey, String sql) {
        Log.info("onException:{},time:{}", sql, (endTime - startTime));
    }
}
