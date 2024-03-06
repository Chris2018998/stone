/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beeop;

/**
 * Pool JMX Bean interface
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeObjectPoolJmxBean {

    //return poolName
    String getPoolName();

    //return current size(using +idle)
    int getTotalSize();

    //return idle size
    int getIdleSize();

    //return using size
    int getUsingSize();

    //return semaphore acquired success size from pool
    int getSemaphoreAcquiredSize();

    //return waiting size to take semaphore synchronizer
    int getSemaphoreWaitingSize();

    //return waiter size for transferred object
    int getTransferWaitingSize();

    //enable Runtime Log
    void setPrintRuntimeLog(boolean indicator);
}