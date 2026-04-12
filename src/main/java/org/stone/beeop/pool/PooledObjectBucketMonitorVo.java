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

import static org.stone.beecp.pool.ConnectionPoolStatics.POOL_RESTART_FAILED;
import static org.stone.beeop.pool.ObjectPoolStatics.*;

/**
 * object pool Monitor Vo
 *
 * @author Chris Liao
 * @version 1.0
 */

public final class PooledObjectBucketMonitorVo implements BeeObjectKeyMonitorVo {
    private final String keyName;
    private final int keyState;
    private final int idleSize;
    private final int borrowedSize;
    private final int creatingSize;
    private final int creatingTimeoutSize;
    private final int semaphoreRemainSize;
    private final int semaphoreWaitingSize;
    private final int transferWaitingSize;
    private final boolean enabledLogPrint;
    private final boolean enabledLogCache;

    public PooledObjectBucketMonitorVo(String keyName,
                                       int keyState,
                                       int idleSize,
                                       int borrowedSize,
                                       int creatingSize,
                                       int creatingTimeoutSize,
                                       int semaphoreRemainSize,
                                       int semaphoreWaitingSize,
                                       int transferWaitingSize,
                                       boolean enabledLogPrint,
                                       boolean enabledLogCache) {
        this.keyName = keyName;
        this.keyState = keyState;
        this.idleSize = idleSize;
        this.borrowedSize = borrowedSize;
        this.creatingSize = creatingSize;
        this.creatingTimeoutSize = creatingTimeoutSize;
        this.semaphoreRemainSize = semaphoreRemainSize;
        this.semaphoreWaitingSize = semaphoreWaitingSize;
        this.transferWaitingSize = transferWaitingSize;
        this.enabledLogPrint = enabledLogPrint;
        this.enabledLogCache = enabledLogCache;
    }

    @Override
    public String getKeyName() {
        return this.keyName;
    }

    @Override
    public boolean isNew() {
        return keyState == POOL_NEW;
    }

    @Override
    public boolean isClosing() {
        return keyState == POOL_CLOSING;
    }

    @Override
    public boolean isClosed() {
        return keyState == POOL_CLOSED;
    }

    @Override
    public boolean isReady() {
        return keyState == POOL_READY;
    }

    @Override
    public boolean isStarting() {
        return keyState == POOL_STARTING;
    }

    @Override
    public boolean isRestarting() {
        return keyState == POOL_RESTARTING;
    }

    @Override
    public boolean isRestartFailed() {
        return keyState == POOL_RESTART_FAILED;
    }

    @Override
    public boolean isSuspended() {
        return keyState == POOL_SUSPENDED;
    }


    @Override
    public int getIdleSize() {
        return idleSize;
    }

    @Override
    public int getBorrowedSize() {
        return borrowedSize;
    }

    @Override
    public int getCreatingSize() {
        return creatingSize;
    }

    @Override
    public int getCreatingTimeoutSize() {
        return creatingTimeoutSize;
    }

    @Override
    public int getSemaphoreRemainSize() {
        return semaphoreRemainSize;
    }

    @Override
    public int getSemaphoreWaitingSize() {
        return semaphoreWaitingSize;
    }

    @Override
    public int getTransferWaitingSize() {
        return transferWaitingSize;
    }

    @Override
    public boolean isEnabledLogPrinter() {
        return enabledLogPrint;
    }

    @Override
    public boolean isEnabledLogCache() {
        return enabledLogCache;
    }
}

