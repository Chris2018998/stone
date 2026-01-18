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
final class ObjectPoolLogCache<K> implements MethodLogCache<K> {
    //Pool name
    private String poolName;
    //Cache size
    private int maxSize;
    //Log type to be cached
    private int logType;
    //Slow Threshold
    private long slowThreshold;
    //Logs queue(ConcurrentLinkedQueue is better than it?)
    private LinkedBlockingQueue<MethodLog<K>> logsQueue;
    //Log listener
    private BeeMethodLogListener<K> listener;

    //***************************************************************************************************************//
    //                                         1: initialization(1+0)                                                //
    //***************************************************************************************************************//
    public void init(String poolName, int logTypeSize, int typeCacheSize, BeeMethodLogListener<K> listener) {
        this.poolName = poolName;
        this.maxSize = typeCacheSize;
        this.listener = listener;
        this.logsQueue = new LinkedBlockingQueue<>(typeCacheSize);
    }

    //***************************************************************************************************************//
    //                                         2: set(2+0)                                                           //
    //***************************************************************************************************************//
    public void setSlowThreshold(int logType, long slowThreshold) {
        this.slowThreshold = slowThreshold;
    }

    public void setLogListener(BeeMethodLogListener<K> listener) {
        this.listener = listener;
    }

    //***************************************************************************************************************//
    //                                         3: Logs records(2+1)                                                  //
    //***************************************************************************************************************//
    public BeeMethodLog<K> beforeCall(long startTime, K key, int logType, String method, Object[] parameters) {
        MethodLog<K> log = new MethodLog<>(poolName, key, logType, method, parameters, startTime);
        this.offerQueue(log);
        if (listener != null) listener.onMethodStart(log);
        return log;
    }

    public void afterCall(long endTime, Object callResult, BeeMethodLog<K> log) {
        MethodLog<K> defaultTypeLog = (MethodLog<K>) log;
        defaultTypeLog.setResult(callResult, endTime);

        if (defaultTypeLog.isRemoved()) {
            defaultTypeLog.setRemoved(false);
            offerQueue(defaultTypeLog);
        }

        defaultTypeLog.setAsSlow(0L, slowThreshold);
        if (listener != null) listener.onMethodEnd(log);
    }

    private void offerQueue(MethodLog<K> log) {
        while (!logsQueue.offer(log)) {
            if (logsQueue.size() == this.maxSize) {
                MethodLog<K> firstLog = logsQueue.poll();
                if (firstLog != null) firstLog.setRemoved(true);
            }
        }
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
        List<BeeMethodLog<K>> longRunningLogList = new ArrayList<>(1);
        List<MethodLog<K>> pendingRemovalLogList = new LinkedList<>();
        long currentTime = System.currentTimeMillis();

        //1: scan log list to find out all timeout logs to be removed
        for (MethodLog<K> log : logsQueue) {
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
            for (MethodLog<K> log : pendingRemovalLogList) {
                log.setRemoved(true);
            }
        }

        //3: handle long-running logs
        if (!longRunningLogList.isEmpty() && listener != null) {
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
