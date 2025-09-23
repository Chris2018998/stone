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

/**
 * A method log represents an activity of a connection get or execution of a SQL.
 *
 * @author Chris Liao
 */
public final class BeeJdbcCallLog implements Serializable {
    public static final int Type_Get_Connection = 1;
    public static final int Type_Execution_SQL = 2;

    //Log type
    private final int type;
    //Method name of pool or (Statement,PreparedStatement,CallableStatement)
    private final String method;
    //Array of method parameters
    private final Object[] parameters;
    //Start time to call method,time unit:milliseconds
    private long startTime;
    //End time of method call,time unit:milliseconds
    private long endTime;

    //A prepared sql or a statement sql.
    private String sql;

    //An elapsed time on prepared sql,refer to {@code connection.prepareStatement(String,...)} method or {@code connection.prepareCall(String,...)}method
    private long preparationTookTime;
    //A parameter array for prepared sql
    private Object[] preparedParameters;

    //Result object of method call
    private Object resultObject;
    //Fail exception to method call
    private Throwable failCause;

    //***************************************************************************************************************//
    //                                          constructor                                                          //
    //***************************************************************************************************************//
    public BeeJdbcCallLog(int type, String method, Object[] parameters) {
        this.type = type;
        this.method = method;
        this.parameters = parameters;
    }

    //***************************************************************************************************************//
    //                                          set/get                                                              //
    //***************************************************************************************************************//
    public int getType() {
        return type;
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

    public String getMethod() {
        return method;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public long getPreparationTookTime() {
        return preparationTookTime;
    }

    public Object[] getPreparedParameters() {
        return preparedParameters;
    }

    public Object getResultObject() {
        return resultObject;
    }

    public Throwable getFailCause() {
        return failCause;
    }

    //***************************************************************************************************************//
    //                                          set result and set exception                                         //
    //***************************************************************************************************************//
    public void setResult(Object callResult, long preparationTookTime, Object[] preparedParameters) {
        this.resultObject = callResult;
        this.preparationTookTime = preparationTookTime;
        this.preparedParameters = preparedParameters;
    }

    public void setException(Throwable failCause, long preparationTookTime, Object[] preparedParameters) {
        this.failCause = failCause;
        this.preparationTookTime = preparationTookTime;
        this.preparedParameters = preparedParameters;
    }
}
