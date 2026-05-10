/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beeop.pool;

import org.stone.beeop.BeeMethodLog;
import org.stone.beeop.BeeMethodLogListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import static org.stone.beeop.BeeMethodLog.Type_Bucket_Log;
import static org.stone.beeop.BeeMethodLog.Type_Object_Log;

/**
 * Method logs cache.
 *
 * @author Chris Liao
 * @version 1.0
 */
class PooledObjectBucketLogCache<K> extends MethodLogCache<K> {
    //Slow Threshold of pooled object get
    private long getSlowThreshold;
    //Slow Threshold of pooled object call
    private long callSlowThreshold;
    //Flag to interrupt slow call logs
    private boolean interruptSlowCall;

    //queue store logs of pooled object get
    private LinkedBlockingQueue<MethodLog<K>> objectGetLogQueue;
    //queue store method call logs of pooled objects
    private LinkedBlockingQueue<MethodLog<K>> objectCallLogQueue;

    //***************************************************************************************************************//
    //                                         1: initialization(1+0)                                                //
    //***************************************************************************************************************//
    public void init(String name, int cacheSize, BeeMethodLogListener<K> listener, boolean enabled) {
        super.init(name, listener, enabled);
        this.objectGetLogQueue = new LinkedBlockingQueue<>(cacheSize);
        this.objectCallLogQueue = new LinkedBlockingQueue<>(cacheSize);
    }

    //***************************************************************************************************************//
    //                                         2: set(2+0)                                                           //
    //***************************************************************************************************************//
    public void setInterruptSlowCall(boolean interruptSlowCall) {
        this.interruptSlowCall = interruptSlowCall;
    }

    public void setSlowThreshold(int logType, long slowThreshold) {
        if (logType == Type_Bucket_Log) {
            this.getSlowThreshold = slowThreshold;
        } else if (logType == Type_Object_Log) {
            this.callSlowThreshold = slowThreshold;
        }
    }

    //***************************************************************************************************************//
    //                                         3: Logs records(2+0)                                                  //
    //***************************************************************************************************************//
    public BeeMethodLog<K> beforeCall(long startTime, K key, int logType, String method, Object[] parameters) throws Exception {
        MethodLog<K> log = new MethodLog<>(poolName, key, logType, method, parameters, startTime);
        if (listener != null) listener.onMethodStart(log);
        if (logType == Type_Bucket_Log) {
            this.offerQueue(log, objectGetLogQueue);
        } else if (logType == Type_Object_Log) {
            this.offerQueue(log, objectCallLogQueue);
        }
        return log;
    }

    public void afterCall(long endTime, Object callResult, BeeMethodLog<K> log) throws Exception {
        MethodLog<K> logImpl = (MethodLog<K>) log;
        logImpl.setResult(callResult, endTime);
        int logType = log.getType();

        if (logImpl.isRemoved()) {
            logImpl.setRemoved(false);
            if (logType == Type_Bucket_Log) {
                this.offerQueue(logImpl, objectGetLogQueue);
            } else if (logType == Type_Object_Log) {
                this.offerQueue(logImpl, objectCallLogQueue);
            }
        }

        if (logType == Type_Bucket_Log) {
            logImpl.setAsSlow(0L, getSlowThreshold, false);
        } else if (logType == Type_Object_Log) {
            logImpl.setAsSlow(0L, callSlowThreshold, false);
        }
        if (listener != null) listener.onMethodEnd(log);
    }

    //***************************************************************************************************************//
    //                                         4: Logs maintain(3+0)                                                 //
    //***************************************************************************************************************//
    public List<BeeMethodLog<K>> getLogs(int logType) {
        if (logType == Type_Bucket_Log) {
            return new ArrayList<>(this.objectGetLogQueue);
        } else if (logType == Type_Object_Log) {
            return new ArrayList<>(this.objectCallLogQueue);
        } else {
            return null;
        }
    }

    public void clearLogs(int logType) {
        if (logType == Type_Bucket_Log) {
            this.objectGetLogQueue.clear();
        } else if (logType == Type_Object_Log) {
            this.objectCallLogQueue.clear();
        }
    }

    public void clearTimeoutLogs(long timeout) {
        clearTimeoutLogsByQueue(timeout, getSlowThreshold, false, objectGetLogQueue);
        clearTimeoutLogsByQueue(timeout, callSlowThreshold, interruptSlowCall, objectCallLogQueue);
    }
}