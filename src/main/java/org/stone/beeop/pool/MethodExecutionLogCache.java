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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Method logs cache.
 *
 * @author Chris Liao
 * @version 1.0
 */
class MethodExecutionLogCache<K> {
    //Pool name
    private String poolName;
    //Cache size
    private int maxSize;
    //Log type to be cached
    private int logType;
    //Slow Threshold
    private long slowThreshold;
    //Logs queue(ConcurrentLinkedQueue is better than it?)
    private LinkedBlockingQueue<MethodExecutionLog<K>> logsQueue;
    //Log listener
    private BeeMethodLogListener<K> listener;

    //***************************************************************************************************************//
    //                                         1: initialization                                                     //
    //***************************************************************************************************************//

    /**
     * initialize log cache.
     *
     * @param poolName      pool name
     * @param cacheSize     is capacity of logs cache
     * @param slowThreshold is slow threshold value,time unit:milliseconds
     * @param listener      is an execution listener
     */
    void initCache(String poolName, int cacheSize, long slowThreshold, BeeMethodLogListener<K> listener) {
        this.poolName = poolName;
        this.maxSize = cacheSize;
        this.listener = listener;
        this.slowThreshold = slowThreshold;
        this.logsQueue = new LinkedBlockingQueue<>(cacheSize);
    }

    public void setMethodExecutionListener(BeeMethodLogListener<K> listener) {
        this.listener = listener;
    }

    //***************************************************************************************************************//
    //                                         2: logs record                                                        //
    //***************************************************************************************************************//
    public BeeMethodLog<K> beforeCall(K key, int logType, String method, Object[] parameters, long startTime) throws Exception {
        MethodExecutionLog<K> log = new MethodExecutionLog<>(poolName, key, logType, method, parameters, startTime);
        this.offerQueue(log);
        if (listener != null) listener.onMethodStart(log);
        return log;
    }

    private void offerQueue(MethodExecutionLog<K> log) {
        while (!logsQueue.offer(log)) {
            if (logsQueue.size() == this.maxSize) {
                MethodExecutionLog<K> firstLog = logsQueue.poll();
                if (firstLog != null) firstLog.setRemoved(true);
            }
        }
    }

    public void afterCall(Object callResult, BeeMethodLog<K> log, long endTime) throws Exception {
        MethodExecutionLog<K> defaultTypeLog = (MethodExecutionLog<K>) log;
        defaultTypeLog.setResult(callResult, endTime);

        if (defaultTypeLog.isRemoved()) {
            defaultTypeLog.setRemoved(false);
            offerQueue(defaultTypeLog);
        }

        defaultTypeLog.setAsSlow(0L, slowThreshold);
        if (listener != null) listener.onMethodEnd(log);
    }

    //***************************************************************************************************************//
    //                                         1: Logs maintain                                                      //
    //***************************************************************************************************************//
    public List<BeeMethodLog<K>> getLogs() {
        return new LinkedList<>(this.logsQueue);
    }

    public void clearLogs() {
        List<MethodExecutionLog<K>> removedLogList = new LinkedList<>();
        for (MethodExecutionLog<K> log : logsQueue) {
            log.setRemoved(true);
            removedLogList.add(log);
        }
        this.logsQueue.removeAll(removedLogList);
    }

    public void clearTimeoutLogs(long timeout) {
        List<BeeMethodLog<K>> longRunningLogList = new ArrayList<>(1);
        List<MethodExecutionLog<K>> pendingRemovalLogList = new LinkedList<>();
        long currentTime = System.currentTimeMillis();

        //1: scan log list to find out all timeout logs to be removed
        for (MethodExecutionLog<K> log : logsQueue) {
            if (currentTime - log.getStartTime() - timeout >= 0L) {//timeout
                pendingRemovalLogList.add(log);
            }

            log.setAsSlow(currentTime, this.slowThreshold);
            if (log.isLongRunning() && !log.hasHandledByListener()) {
                longRunningLogList.add(log);
            }
        }

        //2: remove timeout logs from sql execution log list
        if (!pendingRemovalLogList.isEmpty()) {
            logsQueue.removeAll(pendingRemovalLogList);
            for (MethodExecutionLog<K> log : pendingRemovalLogList) {
                log.setRemoved(true);
            }
        }

        //3: handle long-running logs
        if (!longRunningLogList.isEmpty() && listener != null) {
            try {
                List<Boolean> processFlags = listener.onLongRunningDetected(longRunningLogList);
                if (processFlags != null && !processFlags.isEmpty()) {
                    for (int i = 0, l = processFlags.size(); i < l; i++) {
                        ((MethodExecutionLog<K>) longRunningLogList.get(i)).setHandled(processFlags.get(i).booleanValue());
                    }
                }
            } catch (Throwable e) {
                //log.error();
            }
        }
    }
}
