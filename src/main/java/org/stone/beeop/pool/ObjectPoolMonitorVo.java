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

import org.stone.beeop.BeeObjectPoolMonitorVo;

/**
 * object pool Monitor Vo
 *
 * @author Chris Liao
 * @version 1.0
 */

public final class ObjectPoolMonitorVo implements BeeObjectPoolMonitorVo {
    private final String hostIP;
    private final long threadId;
    private final String threadName;
    private final String poolName;
    private final String poolMode;
    private final int maxSize;
    private int poolState;

    private int idleSize;
    private int borrowedSize;
    private int creatingCount;
    private int creatingTimeoutCount;
    private int semaphoreWaitingSize;
    private int transferWaitingSize;
    private String osId;
    private String osUUID;

    ObjectPoolMonitorVo(
            String poolName, String hostIP, long threadId,
            String threadName, String poolMode, int poolMaxSize) {
        this.poolName = poolName;
        this.hostIP = hostIP;
        this.threadId = threadId;
        this.threadName = threadName;
        this.poolMode = poolMode;
        this.maxSize = poolMaxSize;
    }

    public int getPoolState() {
        return poolState;
    }

    void setPoolState(int poolState) {
        this.poolState = poolState;
    }

    public int getMaxSize() {
        return maxSize;
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

    public int getCreatingCount() {
        return creatingCount;
    }

    void setCreatingCount(int creatingCount) {
        this.creatingCount = creatingCount;
    }

    public int getCreatingTimeoutCount() {
        return creatingTimeoutCount;
    }

    void setCreatingTimeoutCount(int creatingTimeoutCount) {
        this.creatingTimeoutCount = creatingTimeoutCount;
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

    public String getOsId() {
        return osId;
    }

    void setOsId(String osId) {
        this.osId = osId;
    }

    public String getOsUUID() {
        return osUUID;
    }

    void setOsUUID(String osUUID) {
        this.osUUID = osUUID;
    }

    public String getHostIP() {
        return hostIP;
    }

    public long getThreadId() {
        return threadId;
    }

    public String getThreadName() {
        return threadName;
    }

    public String getPoolName() {
        return poolName;
    }

    public String getPoolMode() {
        return poolMode;
    }

    public int getPoolMaxSize() {
        return maxSize;
    }
}

