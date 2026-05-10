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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Method logs cache.
 *
 * @author Chris Liao
 * @version 1.0
 */
final class ObjectPoolLogCache<K> extends MethodLogCache<K> {
    //Slow Threshold
    private long slowThreshold;
    //Logs queue(ConcurrentLinkedQueue is better than it?)
    private LinkedBlockingQueue<MethodLog<K>> logsQueue;

    //***************************************************************************************************************//
    //                                         1: initialization(1+0)                                                //
    //***************************************************************************************************************//
    public void init(String name, int cacheSize, BeeMethodLogListener<K> listener, boolean enabled) {
        super.init(name, listener, enabled);
        this.logsQueue = new LinkedBlockingQueue<>(cacheSize);
    }

    //***************************************************************************************************************//
    //                                         2: field change(0+1)                                                  //
    //***************************************************************************************************************//
    public void setSlowThreshold(int logType, long slowThreshold) {
        this.slowThreshold = slowThreshold;
    }

    //***************************************************************************************************************//
    //                                         3: Logs records(2+1)                                                  //
    //***************************************************************************************************************//
    public BeeMethodLog<K> beforeCall(long startTime, K key, int logType, String method, Object[] parameters) throws Exception {
        MethodLog<K> log = new MethodLog<>(poolName, key, logType, method, parameters, startTime);
        if (listener != null) listener.onMethodStart(log);

        this.offerQueue(log, logsQueue);
        return log;
    }

    public void afterCall(long endTime, Object callResult, BeeMethodLog<K> log) throws Exception {
        MethodLog<K> defaultTypeLog = (MethodLog<K>) log;
        defaultTypeLog.setResult(callResult, endTime);

        if (defaultTypeLog.isRemoved()) {
            defaultTypeLog.setRemoved(false);
            offerQueue(defaultTypeLog, logsQueue);
        }

        defaultTypeLog.setAsSlow(0L, slowThreshold, false);
        if (listener != null) listener.onMethodEnd(log);
    }

    //***************************************************************************************************************//
    //                                         4: Logs maintain(3+0)                                                 //
    //***************************************************************************************************************//
    public List<BeeMethodLog<K>> getLogs(int logType) {
        return new LinkedList<>(this.logsQueue);
    }

    public void clearLogs(int logType) {
        List<MethodLog<K>> removedLogList = new LinkedList<>();
        for (MethodLog<K> log : logsQueue) {
            log.setRemoved(true);
            removedLogList.add(log);
        }
        this.logsQueue.removeAll(removedLogList);
    }

    public void clearTimeoutLogs(long timeout) {
        clearTimeoutLogsByQueue(timeout, slowThreshold, false, logsQueue);
    }
}
