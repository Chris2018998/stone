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
 * Method logs cache.
 *
 * @author Chris Liao
 * @version 1.0
 */
class ObjectKeyLogCache<K> implements MethodLogCache<K> {

    public void init(String poolName, int typeCacheSize, BeeMethodLogListener<K> listener) {

    }

    public void setSlowThreshold(int logType, long slowThreshold) {

    }

    public void setLogListener(BeeMethodLogListener<K> listener) {

    }

    public BeeMethodLog<K> beforeCall(long startTime, K key, int logType, String method, Object[] parameters) {
        return null;
    }

    public void afterCall(long endTime, Object callResult, BeeMethodLog<K> log) {

    }

    public void clearLogs(int logType) {

    }


    public List<BeeMethodLog<K>> getLogs(int logType) {
        return null;
    }

    public void clearTimeoutLogs(long timeout) {

    }
}