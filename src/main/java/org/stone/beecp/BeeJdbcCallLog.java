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

import java.io.Serializable;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 * A method call log is generated from {@link org.stone.beeop.BeeObjectCallLogCollector#startCall(Object, int, String, Object[])}
 *
 * @author Chris Liao
 */
public final class BeeJdbcCallLog implements Serializable {
    public static final int Type_Get_Connection = 1;
    public static final int Type_Execution_SQL = 2;

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
    //Flag of processed by listener
    private boolean processed;

    //***************************************************************************************************************//
    //                                          constructor                                                          //
    //***************************************************************************************************************//
    public BeeJdbcCallLog(int type, String method, Object[] parameters) {
        this.type = type;
        this.method = method;
        this.parameters = parameters;
        this.id = UUID.randomUUID();
    }

    //***************************************************************************************************************//
    //                                          set/get                                                              //
    //***************************************************************************************************************//
    public Object getId() {
        return id;
    }

    public int getType() {
        return type;
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

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
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

    public void setDatasourceInfo(String datasourceInfo) {
        this.datasourceInfo = datasourceInfo;
    }

    //***************************************************************************************************************//
    //                                          sql execution                                                        //
    //***************************************************************************************************************//

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public long getSqlPreparedTime() {
        return sqlPreparedTime;
    }

    public void setSqlPreparedTime(long sqlPreparedTime) {
        this.sqlPreparedTime = sqlPreparedTime;
    }

    public void setStatement(Statement statement) {
        this.statement = statement;
    }

    public Object[] getSqlPreparedParameters() {
        return sqlPreparedParameters;
    }

    public void setSqlPreparedParameters(Object[] sqlPreparedParameters) {
        this.sqlPreparedParameters = sqlPreparedParameters;
    }

    public boolean isSqlExecuting() {
        return this.statement != null && endTime == 0L;
    }

    public void cancelSqlExecuting() throws SQLException {
        if (this.statement != null && endTime == 0L) statement.cancel();
    }

    //***************************************************************************************************************//
    //                                       maintain by log collector                                               //
    //***************************************************************************************************************//
    public boolean isRemoved() {
        return removed;
    }

    public void setRemoved(boolean removed) {
        this.removed = removed;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    //***************************************************************************************************************//
    //                                       set result and set exception                                            //
    //***************************************************************************************************************//
    public void setResult(Object callResult, long sqlPreparedTime, Object[] sqlPreparedParameters) {
        this.resultObject = callResult;
        this.sqlPreparedTime = sqlPreparedTime;
        this.sqlPreparedParameters = sqlPreparedParameters;
        this.statement = null;
    }

    public void setException(Throwable failCause, long sqlPreparedTime, Object[] sqlPreparedParameters) {
        this.failCause = failCause;
        this.sqlPreparedTime = sqlPreparedTime;
        this.sqlPreparedParameters = sqlPreparedParameters;
        this.statement = null;
    }

    //***************************************************************************************************************//
    //                                       Override methods                                                        //
    //***************************************************************************************************************//
    public int hashCode() {
        return this.id.hashCode();
    }

    public boolean equals(Object o) {
        if (o instanceof BeeJdbcCallLog) {
            return this.id.equals(((BeeJdbcCallLog) o).id);
        } else {
            return false;
        }
    }
}
