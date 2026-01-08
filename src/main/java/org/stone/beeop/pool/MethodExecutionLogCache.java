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

import org.stone.beeop.BeeMethodExecutionListener;
import org.stone.beeop.BeeMethodExecutionLog;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import static org.stone.beeop.BeeMethodExecutionLog.Type_Object_Call;
import static org.stone.beeop.BeeMethodExecutionLog.Type_Object_Get;

/**
 * Method logs cache.
 *
 * @author Chris Liao
 * @version 1.0
 */
class MethodExecutionLogCache<K> {
    //pool name
    private String poolName;
    //cache size
    private int maxSize;
    //slow threshold value of objects get,time unit:milliseconds,refer to {@code BeeDataSourceConfig.slowobjectGetThreshold}
    private long slowObjectGetThreshold;
    //slow threshold of sql execution,time unit:milliseconds,refer to {@code BeeDataSourceConfig.slowSQLExecutionThreshold}
    private long slowMethodCallThreshold;

    //queue to store logs of objects getting
    private LinkedBlockingQueue<MethodExecutionLog<K>> objectGetLogsQueue;
    //queue to store logs of methods call
    private LinkedBlockingQueue<MethodExecutionLog<K>> methodsCallLogsQueue;
    //listener to process method execution logs
    private BeeMethodExecutionListener<K> listener;

    //***************************************************************************************************************//
    //                                         1: initialization                                                     //
    //***************************************************************************************************************//

    /**
     * initialize log cache.
     *
     * @param cacheSize is capacity of logs cache
     * @param slowGet   is slow threshold value of object get,time unit:milliseconds
     * @param slowExec  is slow threshold of sql execution,time unit:milliseconds
     * @param listener  is an execution listener
     */
    void initCache(String poolName, int cacheSize, long slowGet, long slowExec, BeeMethodExecutionListener<K> listener) {
        this.poolName = poolName;
        this.listener = listener;
        this.slowObjectGetThreshold = slowGet;
        this.slowMethodCallThreshold = slowExec;

        this.maxSize = cacheSize;
        this.objectGetLogsQueue = new LinkedBlockingQueue<>(cacheSize);
        this.methodsCallLogsQueue = new LinkedBlockingQueue<>(cacheSize);
    }

    public void setMethodExecutionListener(BeeMethodExecutionListener<K> listener) {
        this.listener = listener;
    }

    //***************************************************************************************************************//
    //                                         2: logs record                                                        //
    //***************************************************************************************************************//
    public BeeMethodExecutionLog<K> beforeCall(K key, int type, String method, Object[] parameters) throws Exception {
        MethodExecutionLog<K> log = new MethodExecutionLog<>(key, poolName, type, method, parameters);
        this.offerQueue(log, type);
        if (listener != null) listener.onMethodStart(log);
        return log;
    }

    private void offerQueue(MethodExecutionLog<K> log, int type) {
        LinkedBlockingQueue<MethodExecutionLog<K>> queue;
        if (type == Type_Object_Get) {//object logs
            queue = objectGetLogsQueue;
        } else {//method call logs
            queue = methodsCallLogsQueue;
        }

        while (!queue.offer(log)) {
            if (queue.size() == this.maxSize) {
                MethodExecutionLog<K> firstLog = queue.poll();
                if (firstLog != null) firstLog.setRemoved(true);
            }
        }
    }

    public void afterCall(Object callResult, BeeMethodExecutionLog<K> log) throws Exception {
        MethodExecutionLog<K> defaultTypeLog = (MethodExecutionLog<K>) log;
        defaultTypeLog.setResult(callResult);
        int logType = defaultTypeLog.getType();

        if (defaultTypeLog.isRemoved()) {
            defaultTypeLog.setRemoved(false);
            offerQueue(defaultTypeLog, logType);
        }

        defaultTypeLog.setAsSlow(0L, Type_Object_Get == logType ? this.slowObjectGetThreshold : this.slowMethodCallThreshold);
        if (listener != null) listener.onMethodEnd(log);
    }

    //***************************************************************************************************************//
    //                                         1: Logs maintain                                                      //
    //***************************************************************************************************************//
    public List<BeeMethodExecutionLog<K>> getLog(int type) {
        List<BeeMethodExecutionLog<K>> logList = new LinkedList<>();
        switch (type) {
            case Type_Object_Get: {
                logList.addAll(this.objectGetLogsQueue);
                break;
            }
            case Type_Object_Call: {
                logList.addAll(this.methodsCallLogsQueue);
                break;
            }
            default: {
                logList.addAll(this.objectGetLogsQueue);
                logList.addAll(this.methodsCallLogsQueue);
                break;
            }
        }
        return logList;
    }

    public List<BeeMethodExecutionLog<K>> clear(int type) {
        List<BeeMethodExecutionLog<K>> removedLogList = new LinkedList<>();
        switch (type) {
            case Type_Object_Get: {
                objectGetLogsQueue.drainTo(removedLogList);
                break;
            }
            case Type_Object_Call: {
                methodsCallLogsQueue.drainTo(removedLogList);
                break;
            }
            default: {
                objectGetLogsQueue.drainTo(removedLogList);
                methodsCallLogsQueue.drainTo(removedLogList);
                break;
            }
        }

        for (BeeMethodExecutionLog<K> log : removedLogList)
            ((MethodExecutionLog<K>) log).setRemoved(true);
        return removedLogList;
    }


    public void clearTimeout(long timeout) {
        List<BeeMethodExecutionLog<K>> longRunningLogList = new ArrayList<>(1);
        List<BeeMethodExecutionLog<K>> objectGetPendingRemovalLogList = new LinkedList<>();
        List<BeeMethodExecutionLog<K>> methodCallPendingRemovalLogList = new LinkedList<>();
        long currentTime = System.currentTimeMillis();

        //1: scan log list to find out all timeout logs to be removed
        for (MethodExecutionLog<K> log : objectGetLogsQueue) {
            if (currentTime - log.getStartTime() - timeout >= 0L) {//timeout
                objectGetPendingRemovalLogList.add(log);
            }

            log.setAsSlow(currentTime, this.slowObjectGetThreshold);
            if (log.isLongRunning() && !log.hasHandledByListener()) {
                longRunningLogList.add(log);
            }
        }

        //2: timeout check on method call logs
        for (MethodExecutionLog<K> log : methodsCallLogsQueue) {
            if (currentTime - log.getStartTime() - timeout >= 0L) {
                methodCallPendingRemovalLogList.add(log);
                if (log.getEndTime() == 0L) objectGetPendingRemovalLogList.add(log);
            }

            log.setAsSlow(currentTime, this.slowMethodCallThreshold);
            if (log.isLongRunning() && !log.hasHandledByListener()) {
                longRunningLogList.add(log);
            }
        }

        //3: remove timeout logs from object log list
        if (!objectGetPendingRemovalLogList.isEmpty()) {
            objectGetLogsQueue.removeAll(objectGetPendingRemovalLogList);
            for (BeeMethodExecutionLog<K> log : objectGetPendingRemovalLogList) {
                ((MethodExecutionLog<K>) log).setRemoved(true);
            }
        }

        //4: remove timeout logs from sql execution log list
        if (!methodCallPendingRemovalLogList.isEmpty()) {
            methodsCallLogsQueue.removeAll(methodCallPendingRemovalLogList);
            for (BeeMethodExecutionLog<K> log : methodCallPendingRemovalLogList) {
                ((MethodExecutionLog<K>) log).setRemoved(true);
            }
        }

        //5: handle slow log list
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
