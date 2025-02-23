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
 * Object pool monitor interface.
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface BeeObjectPoolMonitorVo {

    String getPoolName();

    String getPoolMode();

    int getKeySize();

    int getPoolMaxSize();

    int getPoolState();

    int getIdleSize();

    int getBorrowedSize();

    int getCreatingCount();

    int getCreatingTimeoutCount();

    int getSemaphoreWaitingSize();

    int getTransferWaitingSize();

}
