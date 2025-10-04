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

/**
 * Jdbc log interface,its instances generated from {@link BeeJdbcCallLogManager#startCall(int, String, Object[], String, Statement)}
 *
 * @author Chris Liao
 */
public interface BeeJdbcCallLog extends Serializable {
    //constants log type,connections borrow log
    int Type_Get_Connection = 0;
    //constants log type,SQL execution logs
    int Type_Execution_SQL = 1;

    //Method call is in executing
    int Status_Running = 0;
    //Method call is successful
    int Status_Successful = 1;
    //Method call is failed
    int Status_Failed = 2;

    /**
     * Get log type.
     *
     * @return type value,which is one of [Type_Get_Connection,Type_Execution_SQL]
     */
    int getType();

    /**
     * Get Log id.
     *
     * @return log id
     */
    Object getId();

    /**
     * Get method name of method call
     *
     * @return method name of method
     */
    String getMethod();

    /**
     * Get desc info of data source(which may show on a distribution manager).
     *
     * @return desc
     */
    String getDatasourceInfo();

    /**
     * Get method parameter values,which may be null.
     *
     * @return method name of method
     */
    Object[] getParameters();

    /**
     * Get start time to call method.
     *
     * @return start time point,which is milliseconds
     */
    long getStartTime();

    /**
     * Get end time of call method.
     *
     * @return end time point,which is milliseconds
     */
    long getEndTime();


    //***************************************************************************************************************//
    //                                         2: Status and result                                                  //
    //***************************************************************************************************************//

    /**
     * Query status of method call.
     *
     * @return an int value,which one of [Status_Running,Status_Successful,Status_Failed]
     */
    int getStatus();

    /**
     * Query method call is whether slow.
     *
     * @return a boolean,true is slow
     */
    boolean isSlow();

    /**
     * Query method call is whether exception.
     *
     * @return a boolean,true is exception
     */
    boolean isException();

    /**
     * Get result of method call,this result may be null.
     *
     * @return a result object
     */
    Object getResultObject();

    /**
     * Get fail cause of method call,this cause may be null.
     *
     * @return a result object
     */
    Throwable getFailCause();

    /**
     * Query log is whether handled by handler.
     *
     * @return a boolean,true is handled
     */
    boolean isHandled();

    /**
     * Query log is whether removed from log collector.
     *
     * @return a boolean,true is removed,false is the log is still in log collector
     */
    boolean isRemoved();

    //***************************************************************************************************************//
    //                                         3: SQL Execution                                                      //
    //***************************************************************************************************************//

    /**
     * Get execution sql,which may be null,if log type is not {@link #Type_Execution_SQL}.
     *
     * @return log sql
     */
    String getSql();

    /**
     * Get elapsed time on sql preparation.
     *
     * @return elapsed time
     */
    long getSqlPreparedTime();

    /**
     * Get parameter values of preparation sql.
     *
     * @return array of parameter values
     */
    Object[] getSqlPreparedParameters();

    /**
     * Query current log is whether sql in running
     *
     * @return a boolean,true is that current log is of a sql in executing
     */
    boolean isRunningStatement();

    /**
     * Cancel sql statement if in execution.
     */
    void cancelRunningStatement() throws SQLException;
}
