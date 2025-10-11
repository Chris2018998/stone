/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beeop;

import java.io.Serializable;

/**
 * Object event log interface,its instances generated from {@link BeeObjectEventLogManager#startCall(Object, int, String, Object[])}
 *
 * @author Chris Liao
 */
public interface BeeObjectEventLog<K, V> extends Serializable {
    //constants log type,objects borrow log
    int Type_Object_Get = 0;
    //constants log type,object call logs
    int Type_Object_Call = 1;

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
    K getKey();

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
     * Get end time of method call.
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
    V getResultObject();

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
     * Query log is whether removed from log manager.
     *
     * @return a boolean,true is removed,false is the log is still in log manager
     */
    boolean isRemoved();

}
