/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp.pool;

import org.stone.beecp.BeeJdbcCallLog;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 * Default implementation of {@link BeeJdbcCallLog}
 *
 * @author Chris Liao
 */
public class DefaultJdbcCallLog implements BeeJdbcCallLog {
    //Log type
    private final int type;
    //log id
    private final Object id;

    //Method name of pool or (Statement,PreparedStatement,CallableStatement)
    private final String method;
    //Array of method parameters
    private final Object[] parameters;
    //desc of data source(Maybe for centralized manager,it can be a web url)
    private String datasourceInfo;

    //Start time of method call,time unit:milliseconds
    private long startTime;
    //End time of method call,time unit:milliseconds
    private long endTime;
    //End time of method call,time unit:milliseconds
    private int status = Status_Running;

    //Result object of method call
    private Object resultObject;
    //Fail exception when method call
    private Throwable failCause;

    //A prepared sql or a statement sql.
    private String sql;
    //elapsed time on prepared sql,refer to {@code connection.prepareStatement(String,...)} method or {@code connection.prepareCall(String,...)}method
    private long sqlPreparedTime;
    //a parameter array of prepared sql
    private Object[] sqlPreparedParameters;
    //if current log is a sql execution
    private transient Statement statement;


    //Flag of removed from log collector
    private boolean removed;
    //Flag of handled by Handler
    private boolean slow;
    //Flag of handled by Handler
    private boolean handled;

    //***************************************************************************************************************//
    //                                          constructor                                                          //
    //***************************************************************************************************************//
    public DefaultJdbcCallLog(int type, String method, Object[] parameters) {
        this.type = type;
        this.method = method;
        this.parameters = parameters;
        this.id = UUID.randomUUID();
    }

    public int getType() {
        return type;
    }

    public Object getId() {
        return id;
    }

    public String getMethod() {
        return method;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public long getStartTime() {
        return startTime;
    }

    void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public int getStatus() {
        return status;
    }

    public boolean isSlow() {
        return slow;
    }

    void setSlow(boolean slow) {
        this.slow = slow;
    }

    public boolean isException() {
        return this.status == Status_Failed;
    }

    public Object getResultObject() {
        return resultObject;
    }

    public Throwable getFailCause() {
        return failCause;
    }

    public String getDatasourceInfo() {
        return datasourceInfo;
    }

    void setDatasourceInfo(String datasourceInfo) {
        this.datasourceInfo = datasourceInfo;
    }


    public String getSql() {
        return sql;
    }

    void setSql(String sql) {
        this.sql = sql;
    }

    public long getSqlPreparedTime() {
        return sqlPreparedTime;
    }

    void setSqlPreparedTime(long sqlPreparedTime) {
        this.sqlPreparedTime = sqlPreparedTime;
    }

    public Object[] getSqlPreparedParameters() {
        return sqlPreparedParameters;
    }

    void setSqlPreparedParameters(Object[] sqlPreparedParameters) {
        this.sqlPreparedParameters = sqlPreparedParameters;
    }

    public boolean isRunningStatement() {
        return this.statement != null && endTime == 0L;
    }

    public void cancelRunningStatement() throws SQLException {
        if (this.statement != null && endTime == 0L) statement.cancel();
    }

    public boolean isRemoved() {
        return removed;
    }

    void setRemoved(boolean removed) {
        this.removed = removed;
    }

    public boolean isHandled() {
        return handled;
    }

    void setHandled(boolean handled) {
        this.handled = handled;
    }

    void setStatement(Statement statement) {
        this.statement = statement;
    }

    void setResult(Object callResult, long sqlPreparedTime, Object[] sqlPreparedParameters) {
        this.resultObject = callResult;
        this.sqlPreparedTime = sqlPreparedTime;
        this.sqlPreparedParameters = sqlPreparedParameters;
        this.statement = null;
        this.status = Status_Successful;
    }

    void setException(Throwable failCause, long sqlPreparedTime, Object[] sqlPreparedParameters) {
        this.failCause = failCause;
        this.sqlPreparedTime = sqlPreparedTime;
        this.sqlPreparedParameters = sqlPreparedParameters;
        this.statement = null;
        this.status = Status_Failed;
    }
}
