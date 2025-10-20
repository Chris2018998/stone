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
    //All logs
    int Type_All = 0;
    //constants log type,objects borrow log
    int Type_Object_Get = 1;
    //constants log type,object call logs
    int Type_Object_Call = 2;

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
    //                                         2: Status                                                             //
    //***************************************************************************************************************//

    /**
     * Query log owner is whether running.
     *
     * @return a boolean,true is slow
     */
    boolean isRunning();

    /**
     * Query log owner is successful to end call.
     *
     * @return a boolean,true is slow
     */
    boolean isSuccessful();

    /**
     * Query log owner is failed to call.
     *
     * @return a boolean,true is exception
     */
    boolean isException();

    /**
     * Query log is whether slow.
     *
     * @return a boolean,true is slow
     */
    boolean isSlow();

    //***************************************************************************************************************//
    //                                         3: Result                                                             //
    //***************************************************************************************************************//

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
