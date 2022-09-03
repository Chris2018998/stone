/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package org.jmin.beeop;

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

    int getValidTestTimeout();

    long getValidAssumeTime();

    boolean isForceCloseUsingOnClear();

    long getDelayTimeForNextClear();

    long getTimerCheckInterval();

    String getPoolImplementClassName();

    String getObjectFactoryClassName();

    boolean isEnableJmx();
}
