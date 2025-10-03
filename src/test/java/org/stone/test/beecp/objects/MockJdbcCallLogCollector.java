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

import org.stone.beecp.BeeJdbcCallLog;
import org.stone.beecp.BeeJdbcCallLogCollector;
import org.stone.beecp.BeeJdbcCallLogListener;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * A simple tracker implementation
 *
 * @author Chris Liao
 */

public class MockJdbcCallLogCollector implements BeeJdbcCallLogCollector {
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
    //                                         1: init                                                               //
    //***************************************************************************************************************//

    /**
     * initialize collector.
     *
     * @param cacheSize is capacity size of inner container to store log execution logs
     */
    public void init(int cacheSize, long slowGet, long slowExec,
                     boolean listenInSync, BeeJdbcCallLogListener listener) {

    }

    //***************************************************************************************************************//
    //                                         2: Collector maintain                                                 //
    //***************************************************************************************************************//

    /**
     * clear all logs
     */
    public void clear() {

    }

    /**
     * clean logs with timeout
     *
     * @param timeout,if elapsed time is not less than this value,log can be removed from this collector
     */
    public void clear(long timeout) {

    }

    /**
     * get log with type
     *
     * @param type is log type
     * @return a list of
     */
    public List<BeeJdbcCallLog> getLog(int type) {
        return null;
    }

    //***************************************************************************************************************//
    //                                         3: log record                                                         //
    //***************************************************************************************************************//

    /**
     * Start to call a method and a log object is return this start method
     *
     * @param type       is method call type
     * @param method     is method name,for example:getConnection()
     * @param parameters is an array of method parameters
     */
    public BeeJdbcCallLog startCall(int type, String method, Object[] parameters, String preparedSQL, Statement statement) {
        return null;
    }

    /**
     * update result info to log object
     *
     * @param callResult is result of target method call
     * @param log        generated from startCall method
     * @preparedParameters is a parameter array of PreparedSQL or CallableSQL
     */
    public void endCall(Object callResult, long preparationTookTime, Object[] preparedParameters, BeeJdbcCallLog log) {

    }

    /**
     * update exception to log object
     *
     * @param failCause is result of target method call
     * @param log       generated from startCall method
     * @preparedParameters is a parameter array of PreparedSQL or CallableSQL
     */
    public void endOnException(Throwable failCause, long preparationTookTime, Object[] preparedParameters, BeeJdbcCallLog log) {

    }

    /**
     * Cancel statement in executing.
     *
     * @param uuid log uuid key
     */
    public void cancelRunningStatement(Object uuid) throws SQLException {

    }

}
