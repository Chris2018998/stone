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
 * A monitor object of pool runtime info
 *
 * @author Chris Liao
 * @version 1.0
 */
public interface TaskPoolMonitorVo {

    int getPoolState();

    int getTaskCount();

    int getRunningCount();

    long getCompletedCount();

    int getScheduledTaskCount();

}
