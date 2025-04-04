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

public class FastConnectionPoolMonitorVo implements BeeConnectionPoolMonitorVo {
    private String poolName;
    private String poolMode;
    private int poolMaxSize;

    private int poolState;
    private int idleSize;
    private int borrowedSize;
    private int semaphoreWaitingSize;
    private int transferWaitingSize;
    private int creatingCount;
    private int creatingTimeoutCount;

    public String getPoolName() {
        return poolName;
    }

    void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public String getPoolMode() {
        return poolMode;
    }

    void setPoolMode(String poolMode) {
        this.poolMode = poolMode;
    }

    public int getPoolMaxSize() {
        return poolMaxSize;
    }

    void setPoolMaxSize(int poolMaxSize) {
        this.poolMaxSize = poolMaxSize;
    }

    public int getPoolState() {
        return poolState;
    }

    void setPoolState(int poolState) {
        this.poolState = poolState;
    }

    public int getIdleSize() {
        return idleSize;
    }

    void setIdleSize(int idleSize) {
        this.idleSize = idleSize;
    }

    public int getBorrowedSize() {
        return borrowedSize;
    }

    void setBorrowedSize(int borrowedSize) {
        this.borrowedSize = borrowedSize;
    }

    public int getSemaphoreWaitingSize() {
        return semaphoreWaitingSize;
    }

    void setSemaphoreWaitingSize(int semaphoreWaitingSize) {
        this.semaphoreWaitingSize = semaphoreWaitingSize;
    }

    public int getTransferWaitingSize() {
        return transferWaitingSize;
    }

    void setTransferWaitingSize(int transferWaitingSize) {
        this.transferWaitingSize = transferWaitingSize;
    }

    public int getCreatingTimeoutCount() {
        return creatingTimeoutCount;
    }

    void setCreatingTimeoutCount(int creatingTimeoutCount) {
        this.creatingTimeoutCount = creatingTimeoutCount;
    }

    public int getCreatingCount() {
        return creatingCount;
    }

    void setCreatingCount(int creatingCount) {
        this.creatingCount = creatingCount;
    }
}
