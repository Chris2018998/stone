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
 * Bee Object SourceConfig JMX Bean interface
 *
 * @author Chris Liao
 * @version 1.0
 */

public interface BeeObjectSourceConfigJmxBean {

    String getPoolName();

    boolean isFairMode();

    int getInitialSize();

    int getMaxActive();

    int getBorrowSemaphoreSize();

    long getMaxWait();

    long getIdleTimeout();

    long getHoldTimeout();

    int getAliveTestTimeout();

    long getAliveAssumeTime();

    boolean isForceCloseUsingOnClear();

    long getDelayTimeForNextClear();

    long getTimerCheckInterval();

    String getPoolImplementClassName();

    String getObjectFactoryClassName();

    boolean isEnableJmx();
}
