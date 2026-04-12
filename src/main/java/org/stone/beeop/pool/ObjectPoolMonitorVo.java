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
public final class ObjectPoolMonitorVo implements BeeObjectPoolMonitorVo {
    private final String poolName;
    private final int poolState;
    private final boolean enabledLogPrinter;
    private final boolean enabledLogCache;
    private Map<String, BeeObjectKeyMonitorVo> keyMonitorVoMap;

    public ObjectPoolMonitorVo(String poolName,
                               int poolState,
                               boolean enabledLogPrint,
                               boolean enabledLogCache) {
        this.poolName = poolName;
        this.poolState = poolState;
        this.enabledLogPrinter = enabledLogPrint;
        this.enabledLogCache = enabledLogCache;
    }

    @Override
    public String getPoolName() {
        return this.poolName;
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

    @Override
    public boolean isEnabledLogPrinter() {
        return this.enabledLogPrinter;
    }

    @Override
    public boolean isEnabledMethodLogCache() {
        return this.enabledLogCache;
    }

    @Override
    public Map<String, BeeObjectKeyMonitorVo> getKeyMonitorVos() {
        return keyMonitorVoMap;
    }

    @Override
    public BeeObjectKeyMonitorVo getKeyMonitorVo(String keyName) {
        return keyMonitorVoMap == null ? null : keyMonitorVoMap.get(keyName);
    }

    void pubKeyMonitorVo(String keyName, PooledObjectBucketMonitorVo keyMonitorVo) {
        if (keyMonitorVoMap == null) this.keyMonitorVoMap = new HashMap<>(1);
        this.keyMonitorVoMap.put(keyName, keyMonitorVo);
    }
}
