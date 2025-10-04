/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.objects;

import org.stone.beecp.*;
import org.stone.test.beecp.driver.MockConnection;
import org.stone.test.beecp.driver.MockXaConnection;

import javax.sql.XAConnection;
import java.sql.Connection;
import java.util.Collections;
import java.util.List;

/**
 * @author Chris Liao
 */
public class MockBasePoolImplementation implements BeeConnectionPool {

    public void init(BeeDataSourceConfig config) {
        //do nothing
    }

    public Connection getConnection() {
        return new MockConnection();
    }

    public XAConnection getXAConnection() {
        return new MockXaConnection(new MockConnection(), null);
    }

    public Connection getConnection(String username, String password) {
        return new MockConnection();
    }

    public XAConnection getXAConnection(String username, String password) {
        return new MockXaConnection(new MockConnection(), null);
    }

    public void close() {
        //do nothing
    }

    public boolean isClosed() {
        return false;
    }

    public void enableLogPrint(boolean indicator) {
        //do nothing
    }

    public boolean isEnabledLogPrint() {
        return false;
    }

    public void enableJdbcCallLogCollector(boolean enable) {
        //do nothing
    }

    public void enableMethodLogCollector(BeeJdbcCallLogManager connectionTracker) {
        //do nothing
    }

    public boolean isEnabledJdbcCallLogCollector() {
        return false;
    }


    /**
     * Get Jdbc logs with a give type
     *
     * @param type is log type to query
     */
    public List<BeeJdbcCallLog> getJdbcCallLog(int type) {
        return Collections.emptyList();
    }

    /**
     * Clear All logs in log collector.
     */
    public void clearJdbcCallLog() {
    }


    public BeeConnectionPoolMonitorVo getPoolMonitorVo() {
        return null;
    }

    public int getConnectionCreatingCount() {
        return 0;
    }

    public int getConnectionCreatingTimeoutCount() {
        return 0;
    }

    public Thread[] interruptConnectionCreating(boolean interruptTimeout) {
        return null;
    }

    public void clear(boolean forceCloseUsing) {
        //do nothing
    }

    public void clear(boolean forceCloseUsing, BeeDataSourceConfig config) {
        //do nothing
    }
}
