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

import java.util.List;

/**
 * Interface of log cache
 *
 * @author Chris Liao
 */
interface MethodLogCache<K> {

    /**
     * Set a new threshold value for given log type
     *
     * @param logType       is target log type
     * @param slowThreshold is a slow threshold value
     */
    void setSlowThreshold(int logType, long slowThreshold);

    /**
     * Set a new log listener to cache.
     *
     * @param listener to be set to cache
     */
    void setLogListener(BeeMethodLogListener<K> listener);

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
    BeeMethodLog<K> beforeCall(long startTime, K key, int logType, String method, Object[] parameters);

    /**
     * Plugin method: Handles a log of method call.
     *
     * @param endTime    end time of method call
     * @param callResult is result of method call
     * @param log        is a log of method call
     */
    void afterCall(long endTime, Object callResult, BeeMethodLog<K> log);

    /**
     * Clear cached logs of given type
     *
     * @param logType is greater than zero,only clear timeout logs
     */
    void clearLogs(int logType);


    /**
     * Retrieve cached logs by type.
     *
     * @param logType is target type to retrieve
     * @return a log list
     */
    List<BeeMethodLog<K>> getLogs(int logType);

    /**
     * Clear cached logs.
     *
     * @param timeout is greater than zero,only clear timeout logs
     */
    void clearTimeoutLogs(long timeout);

}
