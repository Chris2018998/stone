/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beetp;

/**
 * Task Pool Monitor Vo
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface TaskPoolMonitorVo {

    int getPoolState();

    int getWorkerCount();

    int getTaskHoldingCount();

    int getTaskRunningCount();

    long getTaskCompletedCount();

}
