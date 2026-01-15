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

import static org.stone.beeop.pool.ObjectPoolStatics.*;

/**
 * object pool Monitor Vo
 *
 * @author Chris Liao
 * @version 1.0
 */

public final class ObjectKeyMonitorVo<K> implements BeeObjectKeyMonitorVo<K> {
    private final K key;
    private final int poolState;
    private final int idleSize;
    private final int borrowedSize;
    private final int creatingSize;
    private final int creatingTimeoutSize;
    private final int semaphoreRemainSize;
    private final int semaphoreWaitingSize;
    private final int transferWaitingSize;
    private final boolean enabledLogPrint;

    public ObjectKeyMonitorVo(K key,
                              int poolState,
                              int idleSize,
                              int borrowedSize,
                              int creatingSize,
                              int creatingTimeoutSize,
                              int semaphoreRemainSize,
                              int semaphoreWaitingSize,
                              int transferWaitingSize,
                              boolean enabledLogPrint) {
        this.key = key;
        this.poolState = poolState;
        this.idleSize = idleSize;
        this.borrowedSize = borrowedSize;
        this.creatingSize = creatingSize;
        this.creatingTimeoutSize = creatingTimeoutSize;
        this.semaphoreRemainSize = semaphoreRemainSize;
        this.semaphoreWaitingSize = semaphoreWaitingSize;
        this.transferWaitingSize = transferWaitingSize;
        this.enabledLogPrint = enabledLogPrint;
    }

    @Override
    public K getKey() {
        return key;
    }

    @Override
    public boolean isUncreated() {
        return poolState == POOL_UNCREATED;
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
    public boolean isClosed() {
        return poolState == POOL_CLOSED;
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
    public boolean isReStarting() {
        return poolState == POOL_RESTARTING;
    }

    @Override
    public boolean isSuspended() {
        return poolState == POOL_SUSPENDED;
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
}

