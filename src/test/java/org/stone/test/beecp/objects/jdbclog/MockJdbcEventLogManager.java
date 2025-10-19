/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.objects.jdbclog;

import org.stone.beecp.BeeJdbcEventLog;
import org.stone.beecp.BeeJdbcEventLogHandler;
import org.stone.beecp.BeeJdbcEventLogManager;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * implementation for test
 *
 * @author Chris Liao
 */

public class MockJdbcEventLogManager implements BeeJdbcEventLogManager {
    public void init(int cacheSize, long slowGet, long slowExec,
                     boolean listenInSync, BeeJdbcEventLogHandler listener) {
    }

    public void clearTimeout(long timeout) {
    }

    public List<BeeJdbcEventLog> clear(int timeout) {
        return null;
    }

    public List<BeeJdbcEventLog> getLog(int type) {
        return null;
    }

    public BeeJdbcEventLog startCall(int type, String method, Object[] parameters, String preparedSQL, Statement statement) {
        return null;
    }

    public void endCall(Object callResult, long preparationTookTime, Object[] preparedParameters, BeeJdbcEventLog log) {
    }

    public void endOnException(Throwable failCause, long preparationTookTime, Object[] preparedParameters, BeeJdbcEventLog log) {
    }

    public boolean cancelStatement(Object logId)  {
        return true;
    }
}
