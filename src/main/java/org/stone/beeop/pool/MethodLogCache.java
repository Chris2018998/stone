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
 * Abstract class of method log cache
 *
 * @author Chris Liao
 */
abstract class MethodLogCache<K> {
    //name of key pool
    protected String poolName;
    //log cache size
    protected int logCacheSize;
    //listener of method execution logs
    protected BeeMethodLogListener<K> listener;

    //***************************************************************************************************************//
    //                                         1: initialization(1+0)                                                //
    //***************************************************************************************************************//
    public void init(String poolName, int logCacheSize, BeeMethodLogListener<K> listener) {
        this.poolName = poolName;
        this.logCacheSize = logCacheSize;
        this.listener = listener;
    }

    //***************************************************************************************************************//
    //                                         2: field change(2+0)                                                  //
    //***************************************************************************************************************//

    /**
     * Set a new log listener to cache.
     *
     * @param listener to be set to cache
     */
    public void setLogListener(BeeMethodLogListener<K> listener) {
        this.listener = listener;
    }

    /**
     * Set a new threshold value for given log type
     *
     * @param logType       is target log type
     * @param slowThreshold is a slow threshold value
     */
    abstract void setSlowThreshold(int logType, long slowThreshold);

    //***************************************************************************************************************//
    //                                         3: plugin methods to listen (2+0)                                     //
    //***************************************************************************************************************//

    /**
     * Plugin method: Handles a log of method call.
     *
     * @param startTime  is start time point of method call
     * @param key        is a pooled key
     * @param logType    is log type of method call
     * @param method     is method name of call
     * @param parameters is method parameters of call
     * @return a recorded log
     */
    abstract BeeMethodLog<K> beforeCall(long startTime, K key, int logType, String method, Object[] parameters) throws Exception;

    /**
     * Plugin method: Handles a log of method call.
     *
     * @param endTime    end time of method call
     * @param callResult is result of method call
     * @param log        is a log of method call
     */
    abstract void afterCall(long endTime, Object callResult, BeeMethodLog<K> log) throws Exception;

    //***************************************************************************************************************//
    //                                         4: Maintenance on method logs (2+0)                                   //
    //***************************************************************************************************************//

    /**
     * Retrieve cached logs by type.
     *
     * @param logType is target type to retrieve
     * @return a log list
     */
    abstract List<BeeMethodLog<K>> getLogs(int logType);

    /**
     * Clear cached logs of given type
     *
     * @param logType is greater than zero,only clear timeout logs
     */
    abstract void clearLogs(int logType);

    /**
     * Clear cached logs.
     *
     * @param timeout is greater than zero,only clear timeout logs
     */
    abstract void clearTimeoutLogs(long timeout);

    //***************************************************************************************************************//
    //                                         5: Protected methods(0+2)                                             //
    //***************************************************************************************************************//
    protected void offerQueue(MethodLog<K> log, LinkedBlockingQueue<MethodLog<K>> logsQueue) {
        while (!logsQueue.offer(log)) {
            if (logsQueue.size() == this.logCacheSize) {
                MethodLog<K> firstLog = logsQueue.poll();
                if (firstLog != null) firstLog.setRemoved(true);
            }
        }
    }

    protected void clearTimeoutLogsByQueue(long timeout, long slowThreshold, boolean interruptLongLogs, LinkedBlockingQueue<MethodLog<K>> logsQueue) {
        List<MethodLog<K>> pendingRemovalLogList = null;
        List<BeeMethodLog<K>> longRunningLogList = null;
        long currentTime = System.currentTimeMillis();

        //1: scan log list to find out all timeout logs to be removed
        for (MethodLog<K> log : logsQueue) {
            if (currentTime - log.getStartTime() - timeout >= 0L) {//timeout
                if (pendingRemovalLogList == null) pendingRemovalLogList = new LinkedList<>();
                pendingRemovalLogList.add(log);
            }

            log.setAsSlow(currentTime, slowThreshold, interruptLongLogs);
            if (listener != null && log.isLongRunning() && !log.hasHandledByListener()) {
                if (longRunningLogList == null) longRunningLogList = new LinkedList<>();
                longRunningLogList.add(log);
            }
        }

        //2: remove timeout logs from sql execution log list
        if (pendingRemovalLogList != null) {
            logsQueue.removeAll(pendingRemovalLogList);
            for (MethodLog<K> log : pendingRemovalLogList) {
                log.setRemoved(true);
            }
        }

        //3: handle long-running logs
        if (longRunningLogList != null) {
            try {
                List<Boolean> processFlags = listener.onLongRunningDetected(longRunningLogList);
                if (processFlags != null && !processFlags.isEmpty()) {
                    for (int i = 0, l = processFlags.size(); i < l; i++) {
                        ((MethodLog<K>) longRunningLogList.get(i)).setHandled(processFlags.get(i).booleanValue());
                    }
                }
            } catch (Throwable e) {
                //log.error();
            }
        }
    }
}
