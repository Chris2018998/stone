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
 * Object method log interface.
 *
 * @author Chris Liao
 */
public interface BeeMethodLog<K> extends Serializable {
    //All logs
    int Type_All = 0;

    //Key addition log of key
    int Type_Pool_Log = 1;
    //Object borrowing log on key
    int Type_Key_Log = 2;
    //Method call logs on pooled objects
    int Type_Object_Log = 3;

    /**
     * Get pooled key id.
     *
     * @return log id
     */
    K getKey();

    /**
     * Get pool name of current log
     *
     * @return pool name
     */
    String getPoolName();

    /**
     * Get log type.
     *
     * @return type value,which is one of [Type_Object_Get,Type_Object_Call]
     */
    int getType();

    /**
     * Get Log id.
     *
     * @return log id
     */
    String getId();

    /**
     * Get method name of method call
     *
     * @return method name of method
     */
    String getMethod();

    /**
     * Get method parameter values,which may be null.
     *
     * @return method name of method
     */
    Object[] getParameters();

    /**
     * Get start time of method call.
     *
     * @return start time,which is milliseconds
     */
    long getStartTime();

    /**
     * Get end time of method call.
     *
     * @return end time,which is milliseconds
     */
    long getEndTime();


    //***************************************************************************************************************//
    //                                         2: Status                                                             //
    //***************************************************************************************************************//

    /**
     * Query log owner is whether running.
     *
     * @return a boolean,true is running
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

    /**
     * Query log is in long-running.
     *
     * @return a boolean,true is long-running
     */
    boolean isLongRunning();

    /**
     * Query log is whether handled by listener as a log of long-running.
     *
     * @return a boolean,true is that log has been handled
     */
    boolean hasHandledByListener();

    /**
     * Query log is whether removed from log cache.
     *
     * @return a boolean,true is removed,false is the log is still in log cache
     */
    boolean isRemoved();

    //***************************************************************************************************************//
    //                                         3: Result                                                             //
    //***************************************************************************************************************//

    /**
     * Get result of method call,this result may be null.
     *
     * @return a result object
     */
    Object getResult();

    /**
     * Get fail cause of method call,this cause may be null.
     *
     * @return a result object
     */
    Throwable getFailCause();

}
