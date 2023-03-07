/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under GNU Lesser General Public License v2.1.
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
    private int poolState;

    private int maxSize;
    private int idleSize;
    private int usingSize;
    private int semaphoreWaitingSize;
    private int transferWaitingSize;
    private String osId;
    private String osUUID;

    ObjectPoolMonitorVo(String hostIP, long threadId, String threadName, String poolName, String poolMode, int poolMaxSize) {
        this.hostIP = hostIP;
        this.threadId = threadId;
        this.threadName = threadName;
        this.poolName = poolName;
        this.poolMode = poolMode;
        this.maxSize = poolMaxSize;
    }

    public int getPoolState() {
        return poolState;
    }

    public void setPoolState(int poolState) {
        this.poolState = poolState;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public int getIdleSize() {
        return idleSize;
    }

    public void setIdleSize(int idleSize) {
        this.idleSize = idleSize;
    }

    public int getUsingSize() {
        return usingSize;
    }

    public void setUsingSize(int usingSize) {
        this.usingSize = usingSize;
    }

    public int getSemaphoreWaitingSize() {
        return semaphoreWaitingSize;
    }

    public void setSemaphoreWaitingSize(int semaphoreWaitingSize) {
        this.semaphoreWaitingSize = semaphoreWaitingSize;
    }

    public int getTransferWaitingSize() {
        return transferWaitingSize;
    }

    public void setTransferWaitingSize(int transferWaitingSize) {
        this.transferWaitingSize = transferWaitingSize;
    }

    public String getOsId() {
        return osId;
    }

    public void setOsId(String osId) {
        this.osId = osId;
    }

    public String getOsUUID() {
        return osUUID;
    }

    public void setOsUUID(String osUUID) {
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

