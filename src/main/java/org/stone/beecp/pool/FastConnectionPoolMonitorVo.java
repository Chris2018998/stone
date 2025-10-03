/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp.pool;

import org.stone.beecp.BeeConnectionPoolMonitorVo;

/**
 * Connection pool Monitor impl
 *
 * @author Chris Liao
 * @version 1.0
 */

public final class FastConnectionPoolMonitorVo implements BeeConnectionPoolMonitorVo {
    private final String poolName;
    private final String poolMode;
    private final int maxSize;
    private final int semaphoreSize;

    private int poolState;
    private int idleSize;
    private int borrowedSize;
    private int semaphoreAcquiredSize;
    private int semaphoreWaitingSize;
    private int transferWaitingSize;
    private int creatingCount;
    private int creatingTimeoutCount;

    public FastConnectionPoolMonitorVo(String poolName, String poolMode, int maxSize, int semaphoreSize) {
        this.poolName = poolName;
        this.poolMode = poolMode;
        this.maxSize = maxSize;
        this.semaphoreSize = semaphoreSize;
    }

    @Override
    public String getPoolName() {
        return poolName;
    }

    @Override
    public String getPoolMode() {
        return poolMode;
    }

    @Override
    public int getPoolState() {
        return poolState;
    }

    public void setPoolState(int poolState) {
        this.poolState = poolState;
    }

    @Override
    public int getMaxSize() {
        return maxSize;
    }

    @Override
    public int getSemaphoreSize() {
        return semaphoreSize;
    }

    @Override
    public int getBorrowedSize() {
        return borrowedSize;
    }

    public void setBorrowedSize(int borrowedSize) {
        this.borrowedSize = borrowedSize;
    }

    @Override
    public int getIdleSize() {
        return idleSize;
    }

    public void setIdleSize(int idleSize) {
        this.idleSize = idleSize;
    }

    @Override
    public int getSemaphoreAcquiredSize() {
        return semaphoreAcquiredSize;
    }

    public void setSemaphoreAcquiredSize(int semaphoreAcquiredSize) {
        this.semaphoreAcquiredSize = semaphoreAcquiredSize;
    }

    @Override
    public int getSemaphoreWaitingSize() {
        return semaphoreWaitingSize;
    }

    public void setSemaphoreWaitingSize(int semaphoreWaitingSize) {
        this.semaphoreWaitingSize = semaphoreWaitingSize;
    }

    @Override
    public int getTransferWaitingSize() {
        return transferWaitingSize;
    }

    public void setTransferWaitingSize(int transferWaitingSize) {
        this.transferWaitingSize = transferWaitingSize;
    }

    @Override
    public int getCreatingCount() {
        return creatingCount;
    }

    public void setCreatingCount(int creatingCount) {
        this.creatingCount = creatingCount;
    }

    @Override
    public int getCreatingTimeoutCount() {
        return creatingTimeoutCount;
    }

    public void setCreatingTimeoutCount(int creatingTimeoutCount) {
        this.creatingTimeoutCount = creatingTimeoutCount;
    }
}
