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

    //field value from tracee method end or onException
    private Object preparedKey;
    private String methodSignature;
    private long startTime;
    private long endTime;
    private Throwable failCause;
    private String sql;

    public Object getPreparedKey() {
        return preparedKey;
    }

    public void setPreparedKey(Object preparedKey) {
        this.preparedKey = preparedKey;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public void setMethodSignature(String methodSignature) {
        this.methodSignature = methodSignature;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public Throwable getFailCause() {
        return failCause;
    }

    public void setFailCause(Throwable failCause) {
        this.failCause = failCause;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

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
        this.methodSignature = methodSignature;
        this.startTime = startTime;
        this.endTime = endTime;
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
        this.methodSignature = methodSignature;
        this.startTime = startTime;
        this.endTime = endTime;
        this.sql = sql;
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
        this.methodSignature = methodSignature;
        this.startTime = startTime;
        this.endTime = endTime;
        this.preparedKey = preparedKey;
        this.sql = sql;
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
        this.methodSignature = methodSignature;
        this.startTime = startTime;
        this.endTime = endTime;
        this.preparedKey = null;
        this.sql = sql;
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
        this.methodSignature = methodSignature;
        this.startTime = startTime;
        this.endTime = endTime;
        this.preparedKey = null;
        this.sql = sql;
        this.failCause = e;

    }
}
