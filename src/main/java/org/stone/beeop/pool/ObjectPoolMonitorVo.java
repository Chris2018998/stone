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

import org.stone.beeop.BeeObjectKeyMonitorVo;
import org.stone.beeop.BeeObjectPoolMonitorVo;

import java.util.HashMap;
import java.util.Map;

import static org.stone.beeop.pool.ObjectPoolStatics.*;

/**
 * Object key pool monitor vo
 *
 * @author Chris Liao
 * @version 1.0
 */
public class ObjectPoolMonitorVo<K> implements BeeObjectPoolMonitorVo<K> {
    private final String poolName;
    private final boolean isFairMode;
    private final int maxKeySize;
    private final int poolState;
    private final int maxActiveSizeOfKey;
    private final int semaphoreSizeOfKey;
    private final boolean enabledLogPrint;
    private final boolean enabledLogCache;
    private final boolean usingThreadLocal;
    private final Map<K, BeeObjectKeyMonitorVo<K>> keyMonitorVoMap;

    public ObjectPoolMonitorVo(String poolName,
                               boolean useFairMode,
                               boolean useThreadLocal,
                               int maxKeySize,
                               int maxActiveSizeOfKey,
                               int semaphoreSizeOfKey,
                               int poolState,
                               boolean enabledLogPrint,
                               boolean enabledLogCache) {
        this.poolName = poolName;
        this.poolState = poolState;
        this.isFairMode = useFairMode;
        this.usingThreadLocal = useThreadLocal;
        this.maxKeySize = maxKeySize;
        this.maxActiveSizeOfKey = maxActiveSizeOfKey;
        this.semaphoreSizeOfKey = semaphoreSizeOfKey;
        this.enabledLogPrint = enabledLogPrint;
        this.enabledLogCache = enabledLogCache;
        this.keyMonitorVoMap = new HashMap<>(1);
    }

    //return key pool name
    @Override
    public String getPoolName() {
        return this.poolName;
    }

    //Query pool is whether fair mode
    @Override
    public boolean isFairMode() {
        return this.isFairMode;
    }

    //return capacity size of keys in pool
    @Override
    public int getMaxKeySize() {
        return this.maxKeySize;
    }

    //return capacity size in pool
    @Override
    public int getMaxActiveSizeOfKey() {
        return this.maxActiveSizeOfKey;
    }

    //return capacity size in pool
    @Override
    public int getSemaphoreSizeOfKey() {
        return this.semaphoreSizeOfKey;
    }

    //Query pool is using ThreadLocal
    @Override
    public boolean useThreadLocalOfKey() {
        return this.usingThreadLocal;
    }

    @Override
    public boolean isLazy() {
        return poolState == POOL_LAZY;
    }

    @Override
    public boolean isNew() {
        return poolState == POOL_NEW;
    }

    @Override
    public boolean isClosing() {
        return poolState == POOL_CLOSING;
    }

    @Override
    public boolean isReady() {
        return poolState == POOL_READY;
    }

    @Override
    public boolean isStarting() {
        return poolState == POOL_STARTING;
    }

    @Override
    public boolean isRestarting() {
        return poolState == POOL_RESTARTING;
    }

    @Override
    public boolean isRestartFailed() {
        return poolState == POOL_RESTART_FAILED;
    }

    @Override
    public boolean isSuspended() {
        return poolState == POOL_SUSPENDED;
    }

    //Query log print is whether enabled
    @Override
    public boolean isEnabledLogPrinter() {
        return this.enabledLogPrint;
    }

    //Query method execution log cache is whether enabled
    @Override
    public boolean isEnabledLogCache() {
        return this.enabledLogCache;
    }

    //return monitor vo with a key,return null if given key is not exists in pool
    @Override
    public BeeObjectKeyMonitorVo<K> getKeyMonitorVo(K key) {
        return keyMonitorVoMap.get(key);
    }

    //return monitor vos of pooled keys
    @Override
    public BeeObjectKeyMonitorVo<K>[] getKeyMonitorVos() {
        return keyMonitorVoMap.values().toArray(new BeeObjectKeyMonitorVo[keyMonitorVoMap.size()]);
    }

    void pubKeyMonitorVo(K key, BeeObjectKeyMonitorVo<K> vo) {
        this.keyMonitorVoMap.put(key, vo);
    }
}
