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
import java.util.UUID;

/**
 * A call log represents an activity of getting a pooled object from pool or method execution.
 *
 * @author Chris Liao
 */
public final class BeeObjectCallLog<K, V> implements Serializable {
    public static final int Type_Get_Object = 1;
    public static final int Type_Call_Object = 2;

    //pooled key
    private final K key;
    //uuid
    private final Object id;
    //Log type
    private final int type;
    //Method name of pool or object
    private final String method;
    //Array of method parameters
    private final Object[] parameters;

    //Start time to call method,time unit:milliseconds
    private long startTime;
    //End time of method call,time unit:milliseconds
    private long endTime;

    //Result object of method call
    private V resultObject;
    //Fail exception to method call
    private Throwable failCause;
    //desc of object source
    private String objectSourceInfo;


    //Flag of removed from log collector
    private boolean removed;
    //Flag of processed by listener
    private boolean processed;

    //***************************************************************************************************************//
    //                                          constructor                                                          //
    //***************************************************************************************************************//
    public BeeObjectCallLog(K key, int type, String method, Object[] parameters) {
        this.key = key;
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

    public Object getKey() {
        return key;
    }

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

    public V getResultObject() {
        return resultObject;
    }

    public void setResultObject(V resultObject) {
        this.resultObject = resultObject;
    }

    public Throwable getFailCause() {
        return failCause;
    }

    public void setFailCause(Throwable failCause) {
        this.failCause = failCause;
    }

    public String getObjectSourceInfo() {
        return objectSourceInfo;
    }

    public void setObjectSourceInfo(String objectSourceInfo) {
        this.objectSourceInfo = objectSourceInfo;
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
    //                                       Override methods                                                        //
    //***************************************************************************************************************//
    public int hashCode() {
        return this.id.hashCode();
    }

    public boolean equals(Object o) {
        if (o instanceof BeeObjectCallLog) {
            return this.id.equals(((BeeObjectCallLog<?, ?>) o).id);
        } else {
            return false;
        }
    }
}
